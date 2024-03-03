import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka010.{KafkaUtils, LocationStrategies, ConsumerStrategies}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.kafka010.HasOffsetRanges
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.functions._
import org.apache.log4j.{LogManager, Logger}

object TwitterKafkaStreaming {

  val batchDuration: Int = 15
  val checkpointChemin: String = "/Hadoop/mhgb/datalake/"

  val schema_Kafka = StructType(Array(
    StructField("Zipcode", IntegerType, true),
    StructField("ZipCodeType", StringType, true),
    StructField("City", StringType, true),
    StructField("State", StringType, true)
  ))

  private var trace_consommation: Logger = LogManager.getLogger("Log_Console")

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("YourAppName").setMaster("local[*]")
    val sc = new SparkContext(conf)

    val ssc = getSparkStreamingContext(true, batchDuration, sc)

    val kafkaStreams = getConsommateurKafka(sys.env("BOOTSTRAP_SERVERS"), "", "", sys.env("ZOOKEEPER"), "", Array(""), ssc)

    kafkaStreams.foreachRDD {
      rddKafka =>
        if (!rddKafka.isEmpty()) {

          val offsets = rddKafka.asInstanceOf[HasOffsetRanges].offsetRanges
          val dataStreams = rddKafka.map(record => record.value())

          val ss = SparkSession.builder.config(rddKafka.sparkContext.getConf).enableHiveSupport.getOrCreate()
          import ss.implicits._

          val df_kafka = dataStreams.toDF("tweet_message")

          df_kafka.createOrReplaceGlobalTempView("kafka_events")

          val df_eventsKafka = ss.sql("select * from kafka_events")

          df_eventsKafka.show()

          val df_eventsKafka_2 = df_kafka.withColumn("tweet_message", from_json(col("tweet_message"), schema_Kafka))
            .select(col("tweet_message.*"))

          trace_consommation.info("Persistance des offsets dans Kafka en cours....")
          kafkaStreams.asInstanceOf[CanCommitOffsets].commitAsync(offsets)
          trace_consommation.info("Persistance des offsets dans Kafka terminée avec succès ! :) ")

        }
    }

    kafkaStreams.foreachRDD {
      rddKafka =>
        if (!rddKafka.isEmpty()) {
          val offsets = rddKafka.asInstanceOf[HasOffsetRanges].offsetRanges

          val datastreams = rddKafka.map(event => event.value())

          for(o <- offsets){
            println(s"Le topic lu est : ${o.topic},  la partition est : ${o.partition}, l'offset de début est : ${o.fromOffset}, l'offset de fin est : ${o.untilOffset}")
          }
        }
    }

    ssc.start()
    ssc.awaitTermination()
  }

  def getSparkStreamingContext(checkpointing: Boolean, batchDuration: Int, sc: SparkContext): StreamingContext = {
    val ssc = new StreamingContext(sc, Seconds(batchDuration))
    if (checkpointing) {
      ssc.checkpoint(checkpointChemin)
    }
    ssc
  }

  def getConsommateurKafka(bootStrapServers: String, consumerGroupId: String, consumerReadOrder: String, zookeeper: String,
                           kerberosName: String, topics: Array[String], ssc: StreamingContext) = {
    KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams(bootStrapServers, consumerGroupId, consumerReadOrder, zookeeper, kerberosName))
    )
  }

  def kafkaParams(bootStrapServers: String, consumerGroupId: String, consumerReadOrder: String, zookeeper: String,
                  kerberosName: String): Map[String, Object] = {
    Map[String, Object](
      "bootstrap.servers" -> bootStrapServers,
      "group.id" -> consumerGroupId,
      "auto.offset.reset" -> consumerReadOrder,
      "zookeeper.connect" -> zookeeper,
      "security.protocol" -> "SASL_PLAINTEXT",
      "sasl.kerberos.service.name" -> kerberosName
    )
  }
}

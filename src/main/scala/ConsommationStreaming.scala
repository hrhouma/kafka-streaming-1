import KafkaStreaming._
import SparkBigData._
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.functions._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges}

object ConsommationStreaming {

  val checkpointChemin: String = "/Hadoop/mhgb/datalake/"

  val schema_Kafka = StructType(Array(
    StructField("Zipcode", IntegerType, true),
    StructField("ZipCodeType", StringType, true),
    StructField("City", StringType, true),
    StructField("State", StringType, true)
  ))

  private var trace_consommation: Logger = LogManager.getLogger("Log_Console")

  def main(args: Array[String]): Unit = {

    val ssc = getSparkStreamingContext(true, KafkaStreaming.batchDuration)

    val kafkaStreams = getConsommateurKafka(KafkaStreaming.bootStrapServers, KafkaStreaming.consumerGroupId,
      KafkaStreaming.consumerReadOrder, KafkaStreaming.zookeeper, KafkaStreaming.kerberosName,
      KafkaStreaming.topics, ssc)

    kafkaStreams.foreachRDD {
      rddKafka => {
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
    }

    kafkaStreams.foreachRDD {
      rddKafka => {
        if (!rddKafka.isEmpty()) {

          val offsets = rddKafka.asInstanceOf[HasOffsetRanges].offsetRanges

          val datastreams = rddKafka.map(event => event.value())

          for (o <- offsets) {
            println(s"Le topic lu est : ${o.topic},  la partition est : ${o.partition}, l'offset de début est : ${o.fromOffset}, l'offset de fin est : ${o.untilOffset}")
          }

        }
      }
    }

    ssc.start()
    ssc.awaitTermination()

  }

  def fault_tolerant_SparkStreamingContext(checkpointPath: String): StreamingContext = {

    val ssc2 = getSparkStreamingContext(true, KafkaStreaming.batchDuration)

    val kafkaStreams_cp = getConsommateurKafka(KafkaStreaming.bootStrapServers, KafkaStreaming.consumerGroupId,
      KafkaStreaming.consumerReadOrder, KafkaStreaming.zookeeper, KafkaStreaming.kerberosName,
      KafkaStreaming.topics, ssc2)

    ssc2.checkpoint(checkpointPath)

    ssc2
  }

  def execution_checkPoint(): Unit = {

    val ssc_cp = StreamingContext.getOrCreate(checkpointChemin, () => fault_tolerant_SparkStreamingContext(checkpointChemin))

    val kafkaStreams_cp = getConsommateurKafka(KafkaStreaming.bootStrapServers, KafkaStreaming.consumerGroupId,
      KafkaStreaming.consumerReadOrder, KafkaStreaming.zookeeper, KafkaStreaming.kerberosName,
      KafkaStreaming.topics, ssc_cp)

    kafkaStreams_cp.checkpoint(Seconds(15))

    kafkaStreams_cp.foreachRDD {

      rddKafka => {
        if (!rddKafka.isEmpty()) {

          val offsets = rddKafka.asInstanceOf[HasOffsetRanges].offsetRanges
          val dataStreams = rddKafka.map(record => record.value())

          val ss = SparkSession.builder.config(rddKafka.sparkContext.getConf).enableHiveSupport.getOrCreate()
          import ss.implicits._

          val df_kafka = dataStreams.toDF("tweet_message")

          val df_eventsKafka_2 = df_kafka.withColumn("tweet_message", from_json(col("tweet_message"), schema_Kafka))
            .select(col("tweet_message.*"))

        }

      }

    }

    ssc_cp.start()
    ssc_cp.awaitTermination()

  }

  def streamingCas(): Unit = {

    val ssc = getSparkStreamingContext(true, KafkaStreaming.batchDuration)

    ssc.start()
    ssc.awaitTermination()

  }

}

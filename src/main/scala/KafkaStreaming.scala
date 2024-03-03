import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}

object KafkaStreaming {

  var KafkaParam: Map[String, Object] = Map.empty
  var consommateurKafka: InputDStream[ConsumerRecord[String, String]] = null
  private val trace_kafka: Logger = LogManager.getLogger("Log_Console")

  def getKafkaSparkConsumerParams(kafkaBootStrapServers: String, KafkaConsumerGroupId: String, KafkaConsumerReadOrder: String,
                                  KafkaZookeeper: String, KerberosName: String): Map[String, Object] = {
    KafkaParam = Map(
      "bootstrap.servers" -> kafkaBootStrapServers,
      "group.id" -> KafkaConsumerGroupId,
      "auto.offset.reset" -> KafkaConsumerReadOrder,
      "zookeeper.connect" -> KafkaZookeeper,
      "security.protocol" -> "SASL_PLAINTEXT",
      "sasl.kerberos.service.name" -> KerberosName
    )

    KafkaParam
  }

  def getConsommateurKafka(kafkaBootStrapServers: String, KafkaConsumerGroupId: String, KafkaConsumerReadOrder: String,
                           KafkaZookeeper: String, KerberosName: String,
                           KafkaTopics: Array[String], StreamContext: StreamingContext): InputDStream[ConsumerRecord[String, String]] = {
    try {
      KafkaParam = getKafkaSparkConsumerParams(kafkaBootStrapServers, KafkaConsumerGroupId, KafkaConsumerReadOrder, KafkaZookeeper, KerberosName)

      consommateurKafka = KafkaUtils.createDirectStream[String, String](
        StreamContext,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](KafkaTopics, KafkaParam)
      )

    } catch {
      case ex: Exception =>
        trace_kafka.error(s"Erreur dans l'initialisation du consumer Kafka ${ex.getMessage}")
        trace_kafka.info(s"La liste des paramètres pour la connexion du consommateur Kafka sont : $KafkaParam")
    }

    consommateurKafka
  }
}

import java.io.FileNotFoundException

import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

object SparkBigData {

  // Développement d'applications Big Data en Spark

  var ss: SparkSession = _
  var spConf: SparkConf = _

  private val trace_log: Logger = LogManager.getLogger("Logger_Console")

  /**
   * Fonction qui initialise et instancie une session Spark.
   *
   * @param env C'est une variable qui indique l'environnement sur lequel notre application est déployée.
   *            Si env = true, alors l'application est déployée en local, sinon, elle est déployée sur un cluster.
   * @return La session Spark initialisée.
   */
  def Session_Spark(env: Boolean = true): SparkSession = {
    try {
      if (env) {
        System.setProperty("hadoop.home.dir", "C:/Hadoop/")
        ss = SparkSession.builder
          .master("local[*]")
          .config("spark.sql.crossJoin.enabled", "true")
          // .enableHiveSupport()
          .getOrCreate()
      } else {
        ss = SparkSession.builder
          .appName("Mon application Spark")
          .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
          .config("spark.sql.crossJoin.enabled", "true")
          .enableHiveSupport()
          .getOrCreate()
      }
    } catch {
      case ex: FileNotFoundException =>
        trace_log.error("Nous n'avons pas trouvé le winutils dans le chemin indiqué ", ex)
      case ex: Exception =>
        trace_log.error("Erreur dans l'initialisation de la session Spark ", ex)
    }

    ss
  }

  /**
   * Fonction qui initialise le contexte Spark Streaming.
   *
   * @param env          Environnement sur lequel est déployée notre application. Si true, alors on est en localhost.
   * @param duree_batch  Durée du micro-batch SparkStreamingBatchDuration.
   * @return             Une instance du contexte Streaming initialisée.
   */
  def getSparkStreamingContext(env: Boolean = true, duree_batch: Int): StreamingContext = {
    trace_log.info("Initialisation du contexte Spark Streaming")
    val conf = if (env) {
      new SparkConf().setMaster("local[*]").setAppName("Mon application streaming")
    } else {
      new SparkConf().setAppName("Mon application streaming")
    }

    trace_log.info(s"La durée du micro-batch Spark est définie à : $duree_batch secondes")
    new StreamingContext(conf, Seconds(duree_batch))
  }
}

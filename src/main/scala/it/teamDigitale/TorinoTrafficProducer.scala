package it.teamDigitale

import java.nio.ByteBuffer
import java.security.Timestamp
import java.text.{DateFormat, SimpleDateFormat}
import java.util.TimerTask

import it.teamDigitale.avro.Event

import scala.xml.{NamespaceBinding, NodeSeq, TopScope, XML}
import scala.collection.JavaConverters._

/**
  * Created by fabiana on 23/02/17.
  */
object TorinoTrafficProducer {
  val url = "http://opendata.5t.torino.it/get_fdt"

  import TorinoEventProcessor._

  def run(time:Long): Long = {
    val xml = XML.load(url)
    val traffic_data: NodeSeq = xml \\ "traffic_data"
    val ftd_data = traffic_data \\ "FDT_data"
    val generationTimeString = (traffic_data \\ "@generation_time").text
    val generationTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTimeString).getTime

    if(generationTimestamp > time) {
      val tags = for {
        tag <- ftd_data
      } yield convertEvent(tag, generationTimestamp)

      tags.foreach(println(_))
      generationTimestamp
    } else {
      time
    }


  }

  private def convertEvent(ftd_data: NodeSeq, generationTimestamp: Long) : Event = {

    val lcd1 = (ftd_data \ "@lcd1").text
    val road_LCD = (ftd_data \ "@Road_LCD").text
    val road_name = (ftd_data \ "@Road_name").text
    val offset = (ftd_data \ "@offset").text
    val lat = (ftd_data \ "@lat").text
    val lon = (ftd_data \ "@lng").text
    val latLon = s"$lat-$lon"
    val direction = (ftd_data \ "@direction").text
    val accuracy =(ftd_data \ "@accuracy").text
    val period = (ftd_data \ "@period").text
    val flow = (ftd_data \\ "speedflow" \"@flow").text
    val speed = (ftd_data \\ "speedflow" \"@speed").text

    val attributes: Map[CharSequence, CharSequence] = Map(att_lcd1 -> lcd1,
      att_road_LCD -> road_LCD,
      att_road_name -> road_name,
      att_offset -> offset,
      att_direction -> direction,
      att_accuracy -> accuracy,
      att_period -> period,
      att_flow -> flow,
      att_speed -> speed
    )
    new Event(
      "TorinoFDT",
      generationTimestamp,
      1,
      url.hashCode.toString,
      latLon,
      url,
      ByteBuffer.wrap(ftd_data.toString().getBytes()),
      attributes.asJava)
  }

}

object TorinoEventProcessor{

  val att_lcd1 = stringToCharSequence("FDT_data")
  val att_road_LCD = stringToCharSequence("Road_LCD")
  val att_road_name = stringToCharSequence("Road_name")
  val att_offset = stringToCharSequence("offset")
  val att_direction = stringToCharSequence("direction")
  val att_accuracy = stringToCharSequence("accuracy")
  val att_period = stringToCharSequence("period")
  val att_flow = stringToCharSequence("flow")
  val att_speed = stringToCharSequence("speed")

  def stringToCharSequence(x: String): CharSequence = x.asInstanceOf[CharSequence]
}



package main.scala.overlapping.timeSeries

import breeze.linalg.{DenseMatrix, min, DenseVector}
import breeze.plot._
import org.apache.spark.rdd.RDD
import main.scala.overlapping.containers.{TSInstant, TSConfig, SingleAxisBlock}

/**
 * Created by Francois Belletti on 10/28/15.
 */
object PlotTS {

  def apply[DistanceT](
      timeSeries: RDD[(Int, SingleAxisBlock[DistanceT, TSInstant, DenseVector[Double]])],
      title: Option[String] = None,
      selectSensors: Option[Array[Int]] = None,
      saveToFile: Option[String] = None)
      (implicit tSConfig: TSConfig): Unit = {

    val N = tSConfig.nSamples
    val res = min(N.toDouble, 3600.0)

    /**
     * TODO: incorporate sampling into the main.scala.overlapping block
     */

    val extracted = timeSeries
      .flatMap(x => x._2.map({case (t, v) => v}).data)
      .sample(false, min(res / N.toDouble, 1.0))
      .sortBy(x => x._1)
      .collect()

    val timeVector = DenseVector(extracted.map(_._1.timestamp.getMillis.toDouble))

    val d = extracted.head._2.length

    val f = Figure()

    var plotIndex = 0

    val nSensors = selectSensors.getOrElse(0 until d toArray).length

    for (i <- selectSensors.getOrElse(0 until d toArray)) {

      val p = f.subplot(nSensors, 1, plotIndex)

      plotIndex += 1

      p.ylabel = "sensor " + i
      p.xlabel = "time (ms)"

      val obsVector = DenseVector(extracted.map(_._2(i)))

      p += plot(timeVector, obsVector)
    }

    if(title.isDefined) {
      f.subplot(0).title = title.get
    }

    if(saveToFile.isDefined){
      f.saveas(saveToFile.get)
    }


  }

  def showModel(
      modelCoeffs: Array[DenseMatrix[Double]],
      title: Option[String] = None,
      saveToFile: Option[String] = None): Unit ={

    val f = Figure()

    for(i <- modelCoeffs.indices) {

      val p = f.subplot(modelCoeffs.length, 1, i)
      p += image(modelCoeffs(i), GradientPaintScale[Double](-1.0, 1.0))

    }

    if(title.isDefined) {
      f.subplot(0).title = title.get
    }

    if(saveToFile.isDefined){
      f.saveas(saveToFile.get)
    }

  }

  def showCovariance(
      covMatrix: DenseMatrix[Double],
      title: Option[String] = None,
      saveToFile: Option[String] = None): Unit ={

    val f = Figure()

    val p = f.subplot(0)
    p += image(covMatrix.toDenseMatrix)

    if(title.isDefined) {
      f.subplot(0).title = title.get
    }

    if(saveToFile.isDefined){
      f.saveas(saveToFile.get)
    }

  }

  def showProfile(
      profileMatrix: DenseMatrix[Double],
      title: Option[String] = None,
      saveToFile: Option[String] = None): Unit ={

    val f = Figure()

    val p = f.subplot(0)
    p += image(profileMatrix.t)

    p.ylabel = "Space"
    p.xlabel = "Time"

    if(title.isDefined) {
      f.subplot(0).title = title.get
    }

    if(saveToFile.isDefined){
      f.saveas(saveToFile.get)
    }

  }

  def showUnivModel(
      modelCoeffs: Array[DenseVector[Double]],
      title: Option[String] = None,
      saveToFile: Option[String] = None): Unit ={

    val f = Figure()

    for(i <- modelCoeffs.indices) {

      val p = f.subplot(modelCoeffs.length, 1, i)
      p += image(modelCoeffs(i).toDenseMatrix, GradientPaintScale[Double](-1.0, 1.0))

    }

    if(title.isDefined) {
      f.subplot(0).title = title.get
    }

    if(saveToFile.isDefined){
      f.saveas(saveToFile.get)
    }

  }

}

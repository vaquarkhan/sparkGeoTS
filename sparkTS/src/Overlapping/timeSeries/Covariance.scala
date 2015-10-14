package overlapping.timeSeries

import breeze.linalg._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import overlapping._
import overlapping.containers.SingleAxisBlock

import scala.reflect.ClassTag

/**
 * Created by Francois Belletti on 7/10/15.
 */

/*
Estimator of the instantaneous covariance of the process.
 */
class Covariance[IndexT <: Ordered[IndexT] : ClassTag](
     mean: Option[DenseVector[Double]] = None)
    (implicit config: TSConfig, sc: SparkContext)
  extends SecondOrderEssStat[IndexT, DenseVector[Double], (DenseMatrix[Double], Long)]
  with Estimator[IndexT, DenseVector[Double], DenseMatrix[Double]]{

  val d = config.d
  val bcMean = sc.broadcast(mean.getOrElse(DenseVector.zeros[Double](d)))

  override def kernelWidth = IntervalSize(0, 0)

  override def modelOrder = ModelSize(0, 0)

  override def zero = (DenseMatrix.zeros[Double](d, d), 0L)

  override def kernel(slice: Array[(IndexT, DenseVector[Double])]): (DenseMatrix[Double], Long) = {
    val temp = slice(0)._2 - bcMean.value
    (temp * temp.t, 1L)
  }

  override def reducer(x: (DenseMatrix[Double], Long), y: (DenseMatrix[Double], Long)): (DenseMatrix[Double], Long) ={
    (x._1 + y._1, x._2 + y._2)
  }

  def normalize(r: (DenseMatrix[Double], Long)): DenseMatrix[Double] = {
    r._1.map(_ / r._2.toDouble)
  }

  override def estimate(timeSeries: RDD[(Int, SingleAxisBlock[IndexT, DenseVector[Double]])]):
    DenseMatrix[Double] ={

    normalize(
      timeSeriesStats(timeSeries)
    )

  }


}
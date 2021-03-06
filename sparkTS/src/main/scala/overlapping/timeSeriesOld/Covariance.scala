package main.scala.overlapping.timeSeriesOld

import breeze.linalg._
import main.scala.overlapping.containers._
import org.apache.spark.broadcast.Broadcast

import scala.reflect.ClassTag

/**
 * Created by Francois Belletti on 7/10/15.
 */

object Covariance{

  /**
   *  Estimator of the instantaneous covariance of the process E[X_t transpose(X_t)].
   */
  def apply[IndexT : ClassTag](
      timeSeries: VectTimeSeries[IndexT],
      mean: Option[DenseVector[Double]] = None): DenseMatrix[Double] ={

    val estimator = new Covariance[IndexT](
      timeSeries.config,
      timeSeries.content.context.broadcast(mean))

    estimator.estimate(timeSeries)

  }

}


class Covariance[IndexT : ClassTag](
     config: VectTSConfig[IndexT],
     bcMean: Broadcast[Option[DenseVector[Double]]])
  extends SecondOrderEssStat[IndexT, (DenseMatrix[Double], Long)]
  with Estimator[IndexT, DenseMatrix[Double]]{

  val d = config.dim

  override def selection = config.selection

  override def modelOrder = ModelSize(0, 0)

  override def zero = (DenseMatrix.zeros[Double](d, d), 0L)

  override def kernel(slice: Array[(TSInstant[IndexT], DenseVector[Double])]): (DenseMatrix[Double], Long) = {
    val temp = slice(0)._2 - bcMean.value.getOrElse(DenseVector.zeros[Double](d))
    (temp * temp.t, 1L)
  }

  override def reducer(x: (DenseMatrix[Double], Long), y: (DenseMatrix[Double], Long)): (DenseMatrix[Double], Long) ={
    (x._1 + y._1, x._2 + y._2)
  }

  def normalize(r: (DenseMatrix[Double], Long)): DenseMatrix[Double] = {
    r._1.map(_ / r._2.toDouble)
  }

  override def estimate(timeSeries: VectTimeSeries[IndexT]):
    DenseMatrix[Double] ={

    normalize(
      timeSeriesStats(timeSeries)
    )

  }


}
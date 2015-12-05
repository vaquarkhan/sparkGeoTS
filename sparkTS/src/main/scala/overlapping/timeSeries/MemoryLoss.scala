package main.scala.overlapping.timeSeries

import breeze.linalg.{DenseMatrix, DenseVector}
import main.scala.overlapping._
import main.scala.overlapping.containers.{TSInstant, VectTSConfig, TSConfig}

/**
 * Created by Francois Belletti on 9/24/15.
 */
class MemoryLoss[IndexT <: TSInstant[IndexT]](
  q: Int,
  lossFunction: (Array[DenseMatrix[Double]], Array[(IndexT, DenseVector[Double])], Array[DenseVector[Double]]) => (Double, Array[DenseVector[Double]]),
  config: VectTSConfig[IndexT],
  dim: Option[Int] = None)
extends SecondOrderEssStatMemory[IndexT, DenseVector[Double], Double, Array[DenseVector[Double]]]
{

  val d = dim.getOrElse(config.dim)
  val x = Array.fill(q){DenseMatrix.zeros[Double](d, d)}

  def selection =

  def modelOrder = ModelSize(1, 0)

  def zero = 0.0

  def init = Array.fill(q){DenseVector.zeros[Double](d)}

  def setNewX(newX: Array[DenseMatrix[Double]]) = {
    val maxEigenValue = Stability(newX)

    for(i <- x.indices){
      x(i) := newX(i) / maxEigenValue
    }
  }

  override def kernel(slice: Array[(IndexT, DenseVector[Double])], state: Array[DenseVector[Double]]): (Double, Array[DenseVector[Double]]) = {

    if(slice.length != modelWidth){
      return (0.0, lossFunction(x, slice, state)._2)
    }

    lossFunction(x, slice, state)

  }

  override def reducer(x: Double, y: Double): Double = {x + y}

}

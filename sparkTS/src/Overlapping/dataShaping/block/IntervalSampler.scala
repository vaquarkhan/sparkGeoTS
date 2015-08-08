package overlapping.dataShaping.block

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
 * Created by Francois Belletti on 8/6/15.
 *
 * This class samples out from an RDD and computes the approximate intervals
 * that should be used for an even partitioning.
 *
 */
object IntervalSampler{

  def sampleAndComputeIntervals[T: ClassTag](nIntervals: Int,
                                             sampleSize: Int,
                                             withReplacement: Boolean,
                                             sourceRDD: RDD[T])
                               (implicit ordering: Ordering[T]): Array[(T, T)] = {
    val fraction    = sampleSize.toDouble / sourceRDD.count().toDouble

    val stride = sampleSize / nIntervals

    val sortedKeys: Array[T]  = sourceRDD
      .sample(withReplacement, fraction)
      .collect()
      .sortWith(ordering.lt)
      .sliding(1, stride)
      .map(_.apply(0))
      .toArray

    sortedKeys.zip(sortedKeys.drop(1))
  }

}
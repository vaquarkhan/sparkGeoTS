package sandBox

/**
 * Created by cusgadmin on 6/9/15.
 */

import breeze.linalg._
import breeze.numerics.abs
import breeze.plot.{Figure, image}
import breeze.stats.distributions.Gaussian
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import overlapping._
import overlapping.containers._
import overlapping.timeSeries._

object BayesianARp {

  implicit def signedDistMillis = (t1: TSInstant, t2: TSInstant) => (t2.timestamp.getMillis - t1.timestamp.getMillis).toDouble

  implicit def signedDistLong = (t1: Long, t2: Long) => (t2 - t1).toDouble

  def main(args: Array[String]): Unit = {

    val d = 1
    val N = 10000L
    val paddingMillis = 100L
    val deltaTMillis = 1L
    val nPartitions = 8

    implicit val config = TSConfig(deltaTMillis, d, N, paddingMillis.toDouble)

    val conf = new SparkConf().setAppName("Counter").setMaster("local[*]")
    implicit val sc = new SparkContext(conf)

    val p = 3
    val ARCoeffs = Array.fill(p){DenseMatrix.rand[Double](d, d) - 0.5}

    val maxEigen = Stability(ARCoeffs)

    println(maxEigen)

    while(Stability(ARCoeffs) >= 1.0) {
      println(Stability(ARCoeffs))
      for (i <- 0 until p) {
        ARCoeffs(i) :/= (i + 1) * 1.2
      }
    }

    println(Stability(ARCoeffs))

    val noiseMagnitudes = DenseVector.ones[Double](d) + (DenseVector.rand[Double](d) * 0.2)

    val rawTS = IndividualRecords.generateVAR(
      ARCoeffs,
      d,
      N.toInt,
      deltaTMillis,
      Gaussian(0.0, 1.0),
      noiseMagnitudes,
      sc)

    val f0 = Figure()
    f0.subplot(0) += image(ARCoeffs(0))
    f0.saveas("AR_coeffs.png")

    val f1 = Figure()
    f1.subplot(0) += image(diag(noiseMagnitudes))
    f1.saveas("noise_magnitudes.png")

    val (overlappingRDD: RDD[(Int, SingleAxisBlock[TSInstant, DenseVector[Double]])], _) =
      SingleAxisBlockRDD((paddingMillis, paddingMillis), nPartitions, rawTS)



    /*
    ################################

    Monovariate analysis

    ################################
     */
    val mean = MeanEstimator(overlappingRDD)
    val autocovariances = AutoCovariances(overlappingRDD, p)
    val vectorsAR = ARModel(overlappingRDD, p, Some(mean))
    val residualsAR = ARPredictor(overlappingRDD, vectorsAR.map(x => x.covariation), Some(mean))
    val residualSecondMomentAR = SecondMomentEstimator(residualsAR)

    println("AR error")
    println(trace(residualSecondMomentAR))
    println()

    val f2 = Figure()
    f2.subplot(0) += image(residualSecondMomentAR)
    f2.saveas("cov_freq_AR_residuals.png")

    /*
    ##################################

    Multivariate analysis

    ##################################
     */

    val (freqVARMatrices, _) = VARModel(overlappingRDD, p)

    println("Frequentist L1 estimation error")
    println(sum(abs(freqVARMatrices(0) - ARCoeffs(0))))
    println()

    val f4 = Figure()
    f4.subplot(0) += image(freqVARMatrices(0))
    f4.saveas("frequentist_VAR_coeffs.png")

    val residualFrequentistVAR = VARPredictor(overlappingRDD, freqVARMatrices, Some(mean))
    val residualSecondMomentFrequentistVAR = SecondMomentEstimator(residualFrequentistVAR)

    println("Frequentist VAR residuals")
    println(trace(residualSecondMomentFrequentistVAR))
    println()

    val denseVARMatrices = VARGradientDescent(overlappingRDD, p)

    println("Bayesian estimation error")
    println(sum(abs(denseVARMatrices(0) - ARCoeffs(0))))
    println()

    val f5 = Figure()
    f5.subplot(0) += image(denseVARMatrices(0))
    f5.saveas("bayesian_VAR_coeffs.png")

    val residualsBayesianVAR = VARPredictor(overlappingRDD, denseVARMatrices, Some(mean))
    val residualSecondMomentBayesianVAR = SecondMomentEstimator(residualsBayesianVAR)

    println("Bayesian VAR residuals")
    println(trace(residualSecondMomentBayesianVAR))
    println()

    val f6 = Figure()
    f6.subplot(0) += image(residualSecondMomentBayesianVAR)
    f6.saveas("cov_bayesian_VAR_residuals.png")

    /*
    ################################

    Sparse Bayesian analysis

    ################################
     */

    val sparseVARMatrices = VARL1GradientDescent(overlappingRDD, p, 1e-2)

    println("Sparse Bayesian L1 estimation error")
    println(sum(abs(sparseVARMatrices(0) - ARCoeffs(0))))
    println()
    val f7 = Figure()
    f7.subplot(0) += image(sparseVARMatrices(0))
    f7.saveas("sparse_VAR_coeffs.png")

    val residualsSparseVAR = VARPredictor(overlappingRDD, sparseVARMatrices, Some(mean))
    val residualSecondMomentSparseVAR = SecondMomentEstimator(residualsSparseVAR)

    println("Sparse VAR residuals")
    println(trace(residualSecondMomentSparseVAR))
    println()

    val f8 = Figure()
    f8.subplot(0) += image(residualSecondMomentSparseVAR)
    f8.saveas("cov_sparse_VAR_residuals.png")


  }
}
package qgame.akka.cluster

import akka.actor.ActorSystem
import akka.cluster.{ Metric, SigarMetricsCollector }

/**
 * Created by kerr.
 */
class LoadMetricsCollector(system: ActorSystem) extends SigarMetricsCollector(system) {
  private val maxMemory = Runtime.getRuntime.maxMemory()
  private val maxMemoryMetric = Metric.create("memory-max", maxMemory, None).getOrElse(
    throw new IllegalArgumentException("could not create max memory metric")
  )
  override def metrics: Set[Metric] = super.metrics + maxMemoryMetric
}

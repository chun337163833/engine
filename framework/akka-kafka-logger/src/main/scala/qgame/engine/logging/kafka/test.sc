import akka.stream.actor.WatermarkRequestStrategy

val strategy =  WatermarkRequestStrategy(-100,-1000)

strategy.requestDemand(100)

strategy.highWatermark

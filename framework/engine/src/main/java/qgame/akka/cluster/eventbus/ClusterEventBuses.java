package qgame.akka.cluster.eventbus;

import akka.actor.ActorSystem;

/**
 * Created by kerr.
 */
public class ClusterEventBuses {
    public static ClusterActorEventBus getClusterRoleBasedEventbus(ActorSystem system){
        return ClusterActorEventBus$.MODULE$.get(system).get();
    }

    public static ClusterPubSubBackendEventBus getClusterPubSubBackendEventBus(ActorSystem system){
        return ClusterPubSubBackendEventBus$.MODULE$.get(system).get();
    }

}

package qgame.akka.cluster.router;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import scala.concurrent.Future;

/**
 * Created by kerr.
 */
public class RoleRouters {
    private final ActorSystem actorSystem;
    private final ActorRef actorRef;

    private RoleRouters(ActorSystem actorSystem, ActorRef actorRef) {
        this.actorSystem = actorSystem;
        this.actorRef = actorRef;
    }

    public static RoleRouterBuilder get(ActorSystem actorSystem){
        return new RoleRouterBuildImpl(actorSystem);
    }

    public void tell(String role,Object msg,ActorRef sender){
        System.out.println("send out message :"+msg);
        actorRef.tell(new Protocal.SendToRole(role,msg),sender);
    }

    public Future<Object> ask(String role,Object msg, long timeoutMillis){
        return akka.pattern.Patterns.ask(actorRef, new Protocal.SendToRole(role, msg),timeoutMillis);
    }

    private static class RoleRouterBuildImpl implements RoleRouterBuilder {
        private final ActorSystem actorSystem;
        private ActorRef receiver;

        public RoleRouterBuildImpl(ActorSystem actorSystem) {
            this.actorSystem = actorSystem;
        }


        @Override
        public RoleRouterBuilder withReceiver(ActorRef receiver) {
            this.receiver = receiver;
            return this;
        }

        @Override
        public RoleRouters start() {
            ActorRef roleRouterActor = actorSystem.actorOf(Props.create(RoleRouterActor.class,receiver),"roleRouter");
            return new RoleRouters(actorSystem,roleRouterActor);
        }
    }

    public static interface RoleRouterBuilder {
        RoleRouterBuilder withReceiver(ActorRef receiver);
        RoleRouters start();
    }


}

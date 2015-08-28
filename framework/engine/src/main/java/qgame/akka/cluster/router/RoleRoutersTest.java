package qgame.akka.cluster.router;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by kerr.
 */
public class RoleRoutersTest {
    //ping ,pong
    public static void main(String[] args) throws InterruptedException {
        Address joinAddress = startRole(null, "ping", "system", new StartingListener() {
            @Override
            public void onStarting(ActorSystem system) {
                //create one actor,and then starting to send the message to another actor
                system.actorOf(Props.create(PingActor.class),"pingActor");
            }
        });
        System.out.println(joinAddress);
        Thread.sleep(5000);
        startRole(joinAddress, "pong", "system", new StartingListener() {
            @Override
            public void onStarting(ActorSystem system) {
                system.actorOf(Props.create(PongActor.class),"pongActor");
            }
        });
        Thread.sleep(5000);
    }

    public static Address startRole(Address address,String role,String systemName,StartingListener listener){
        Config conf = ConfigFactory.parseString("akka.cluster.roles=[" + role + "]").
                withFallback(ConfigFactory.load());
        ActorSystem system = ActorSystem.create(systemName, conf);
        Cluster cluster=  Cluster.get(system);
        Address realJoinAddress = address == null?cluster.selfAddress():address;
        cluster.join(realJoinAddress);
        if (listener != null){
            listener.onStarting(system);
        }
        System.out.println(">>>self role"+ cluster.getSelfRoles());
        return realJoinAddress;
    }

    public static interface StartingListener{
        void onStarting(ActorSystem system);
    }
    public static class Pong implements Serializable{
        private final String name="pong";

        public Pong() {
        }
    }

    public static class Ping implements Serializable{
        private final String name="ping";

        public Ping() {
        }
    }
    public static class PingActor extends UntypedActor{
        private final RoleRouters roleRouters;
        public PingActor() {
            roleRouters = RoleRouters.get(getContext().system()).withReceiver(getSelf()).start();
        }

        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println("PingActor :"+message);
            if (message instanceof SendTick) {
                SendTick sendTick = (SendTick) message;
                roleRouters.tell("pong",new Ping(),getSelf());
                return;
            }
            if (message instanceof Pong) {
                Pong pong = (Pong) message;
                System.out.println("!!!!!!!! >>"+pong);
            }
        }

        @Override
        public void preStart() throws Exception {
            getContext().system().scheduler().schedule(new FiniteDuration(2, TimeUnit.SECONDS),
                    new FiniteDuration(2, TimeUnit.SECONDS),getSelf(),new SendTick(),getContext().system().dispatcher(),getSelf());
            super.preStart();
        }

        public static class SendTick{
            private final String name = "tick";

            public SendTick() {
            }
        }
    }

    public static class PongActor extends UntypedActor{
        private final RoleRouters roleRouters;

        public PongActor() {
            roleRouters = RoleRouters.get(getContext().system()).withReceiver(getSelf()).start();
        }

        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println("PongActor :"+message);
            if (message instanceof Ping) {
                getSender().tell(new Pong(),getSelf());
            }
        }
    }
}

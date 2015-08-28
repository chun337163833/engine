package qgame.akka.cluster.router

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{ Cluster, Member, MemberStatus }
import qgame.akka.cluster.router.Protocal._

/**
 * Created by kerr.
 */
class RoleRouterActor(
    receiver: ActorRef
) extends Actor with ActorLogging {
  var nodes: Set[Address] = Set.empty
  //role->address
  var registry: Map[String, NodeBucket] = Map.empty.withDefault(role => NodeBucket(Set.empty))
  val cluster = Cluster.get(context.system)
  //address -> roles
  var roles: Map[Address, Set[String]] = Map.empty.withDefault(address => Set.empty)

  def handleMemberUp(m: Member): Unit = {
    nodes += m.address
    roles += m.address -> m.roles
    //after one new node is up,add the information to registry
    m.roles.foreach {
      role =>
        val bucket = registry(role)
        registry += role -> NodeBucket(bucket.nodes + RoleNode(m.address, NodeState.Up))
    }
    //when one node is up,need to getClusterRoleBasedEventbus it ref
    context.actorSelection(self.path.toStringWithAddress(m.address)) ! GetRealRef
  }

  def handleMemberRemoved(m: Member): Unit = {
    nodes -= m.address
    roles -= m.address
    m.roles.foreach(role => registry -= role)
  }

  def handleReachableMember(m: Member): Unit = {
    m.roles.foreach {
      role =>
        val bucket = registry(role)
        val newNodes = bucket.nodes.filterNot(_.node == m.address) + RoleNode(m.address, NodeState.Up)
        registry += role -> NodeBucket(newNodes)
    }
  }

  def handleUnreachableMember(m: Member): Unit = {
    m.roles.foreach {
      role =>
        val bucket = registry(role)
        val newNodes = bucket.nodes.filterNot(_.node == m.address) + RoleNode(m.address, NodeState.Unreachable)
        registry += role -> NodeBucket(newNodes)
    }
  }

  override def receive: Receive = {
    case MemberUp(m) =>
      handleMemberUp(m)
    case MemberRemoved(m, _) =>
      handleMemberRemoved(m)
    case ReachableMember(m) => //mark the member ok
      handleReachableMember(m)
    case UnreachableMember(m) => //mark the member not ok
      handleUnreachableMember(m)
    case state: CurrentClusterState =>
      state.members.foreach {
        member =>
          if (member.status == MemberStatus.up) {
            handleMemberUp(member)
          } else if (member.status == MemberStatus.removed) {
            handleMemberRemoved(member)
          }
      }
    case SendToRole(role, msg) => //for ward to other actor
      registry.get(role) match {
        case None => sender() ! NodeNotRegistered(role, msg)
        case Some(NodeBucket(roleNodes)) =>
          if (roleNodes.forall(roleNode => roleNode.state == NodeState.Removed || roleNode.state == NodeState.Unreachable)) {
            //no one ok
            sender() ! NodeNotAvailable(role, roleNodes)
          } else {
            //some one is ok,then forward out the message
            roleNodes.filter(_.state == NodeState.Up).foreach {
              roleNode =>
                roleNode.ref.foreach(_ forward msg)
            }
          }
      }
    case GetRealRef =>
      sender() ! RealRef(cluster.selfRoles, cluster.selfAddress, self)
    case RealRef(remoteNodeRoles, address, ref) =>
      //we got the remote node's real ref
      log.debug("realRef got,sender :{},ref:{}", sender(), ref)
      remoteNodeRoles.foreach {
        role =>
          val bucket = registry(role)
          val newNodes = bucket.nodes.filter(_.node == address).map(_.copy(ref = Some(ref))) ++ bucket.nodes.filterNot(_.node == address)
          registry += role -> NodeBucket(newNodes)
      }
    case msg => receiver forward msg
  }

  def tellMyRole(address: Address): Unit = {
    context.actorSelection(self.path.toStringWithAddress(address))
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent], classOf[ReachabilityEvent])
    super.preStart()
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    super.postStop()
  }
}

case class NodeBucket(nodes: Set[RoleNode])
case class RoleNode(node: Address, state: NodeState.NodeState, ref: Option[ActorRef] = None)

object NodeState extends Enumeration {
  type NodeState = Value
  val Up, Unreachable, Removed = Value
}

object Protocal {
  case class SendToRole(role: String, msg: AnyRef)
  case object ListAllRole
  case class NodeNotRegistered(role: String, msg: AnyRef)
  case class NodeNotAvailable(role: String, nodes: Set[RoleNode])
  case object GetRealRef
  case class RealRef(role: Set[String], address: Address, realRef: ActorRef)
}

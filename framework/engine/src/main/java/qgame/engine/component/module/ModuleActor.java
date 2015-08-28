package qgame.engine.component.module;

import akka.actor.UntypedActor;
import akka.japi.Procedure;

/**
 * Created by DongLei on 2014/10/14.
 */
public abstract class ModuleActor extends UntypedActor {

    protected Module2 gameModule;
    public static final OperationStatus OPERATION_SUCCESS = new OperationStatus(true);
    public static final OperationStatus OPERATION_FAILED = new OperationStatus(false);

    public ModuleActor(Module2 gameModule) {
        this.gameModule = gameModule;
    }



//    @Override
//    public void onReceive(Object message) throws Exception {
//        if (message instanceof OpenModule) {
//            getSender().tell(OPERATION_SUCCESS,getSelf());
//        }else if(message instanceof CloseModule) {
//            getSender().tell(OPERATION_FAILED,getSelf());
//        }
//    }

    public static class OpenModule {
    }

    public static class CloseModule {
    }

    public static class OperationStatus {
        boolean isSuccess = false;

        OperationStatus(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }
    }

    public void switchBehavior(Object message, Procedure<Object> active, Procedure<Object> inactive) {
        if (message instanceof OpenModule) {
            getContext().become(active);
            getSender().tell(OPERATION_SUCCESS,getSelf());
        }else if (message instanceof CloseModule) {
            getContext().become(inactive);
            getSender().tell(OPERATION_SUCCESS,getSelf());
        }
    }

//    @Override
//    public void preStart() throws Exception {
//        System.out.println("module actor start :"+getSelf());
//        super.preStart();
//    }
//
//    @Override
//    public void postStop() throws Exception {
//        System.out.println("module actor stop :"+getSelf());
//        super.postStop();
//    }


}

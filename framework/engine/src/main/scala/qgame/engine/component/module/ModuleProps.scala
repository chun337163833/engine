package qgame.engine.component.module

import java.lang.reflect.{ Constructor, Modifier }

import akka.actor.SupervisorStrategy
import akka.event.LoggingAdapter
import qgame.engine.libs.Reflect

import scala.annotation.varargs
import scala.collection.immutable
import scala.collection.immutable._
import scala.reflect.ClassTag

/**
 * Created by kerr.
 */

object ModuleProps {
  @varargs
  def create(clazz: Class[_ <: Module], args: AnyRef*): ModuleProps = {
    ModuleProps(clazz, args: _*)
  }

  def apply[T <: Module: ClassTag]: ModuleProps = {
    ModuleProps(DefaultModuleRuntimeProps, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[_ <: Module]], List.empty)
  }

  def apply(clazz: Class[_ <: Module], args: Any*): ModuleProps = {
    ModuleProps(DefaultModuleRuntimeProps, clazz, args.toList)
  }

}

case class ModuleProps(
    runtimeProps: ModuleRuntimeProps,
    clazz: Class[_ <: Module],
    args: immutable.Seq[Any]
) {
  if (Modifier.isAbstract(clazz.getModifiers)) {
    throw new IllegalArgumentException(s"module class [${clazz.getName}] must not be abstract")
  }
  @transient
  private[this] var _creator: IndirectModuleCreator = _
  @transient
  private[this] var _cachedModuleClass: Class[_ <: Module] = _

  private[this] def creator: IndirectModuleCreator = {
    if (_creator eq null) {
      _creator = IndirectModuleCreator(clazz, args)
    }
    _creator
  }

  private[this] def cachedModuleClass: Class[_ <: Module] = {
    if (_cachedModuleClass eq null) {
      _cachedModuleClass = creator.moduleClass
    }
    _cachedModuleClass
  }

  //get the creator
  //creator

  def moduleClass(): Class[_ <: Module] = cachedModuleClass

  private[engine] def newModule(): Module = {
    creator.create()
  }

  def withSupervisorStrategy(provider: LoggingAdapter => SupervisorStrategy): ModuleProps = {
    val newRuntimeModuleProps =
      ConfigurableModuleRuntimeProps(
        dispatcherClazz = runtimeProps.dispatcherClazz,
        mailboxClazz = runtimeProps.mailboxClazz,
        supervisorStrategyProvider = provider
      )
    this.copy(runtimeProps = newRuntimeModuleProps)
  }

  def withSuperVisorStrategy(provider: SuperVisorStrategyProvider): ModuleProps = {
    withSupervisorStrategy(provider.create)
  }
}

trait IndirectModuleCreator {
  def create(): Module

  def moduleClass: Class[_ <: Module]
}

private[this] object IndirectModuleCreator {
  def apply(clazz: Class[_], args: immutable.Seq[Any]): IndirectModuleCreator = {
    if (classOf[Module].isAssignableFrom(clazz)) {
      if (args.isEmpty) {
        new NoArgsReflectConstructor(clazz.asInstanceOf[Class[_ <: Module]])
      } else {
        new ArgsReflectConstructor(clazz.asInstanceOf[Class[_ <: Module]], args)
      }
    } else {
      throw new IllegalArgumentException(s"bad module creator class,please check [$clazz]")
    }
  }
}

private class ArgsReflectConstructor(clz: Class[_ <: Module], args: immutable.Seq[Any]) extends IndirectModuleCreator {
  private[this] val constructor: Constructor[_] = Reflect.findConstructor(clz, args)

  override def moduleClass = clz

  override def create() = Reflect.instantiate(constructor, args).asInstanceOf[Module]
}

class NoArgsReflectConstructor(clz: Class[_ <: Module]) extends IndirectModuleCreator {

  override def moduleClass = clz

  override def create() = {
    //move it here for lazy check
    Reflect.findConstructor(clz, List.empty)
    Reflect.instantiate(clz)
  }
}

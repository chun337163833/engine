package qgame.engine.libs

import java.lang.annotation.Annotation
import java.lang.reflect.{ TypeVariable, GenericArrayType, ParameterizedType, Constructor }

import org.reflections.util.{ ClasspathHelper, ConfigurationBuilder }
import org.reflections.{ ReflectionUtils, Reflections }

import scala.annotation.varargs
import scala.collection.immutable
import scala.language.{ existentials, implicitConversions }
import scala.util.Try
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
object Reflect {

  val getCallerClass: Option[Int ⇒ Class[_]] = {
    try {
      val c = Class.forName("sun.reflect.Reflection")
      val m = c.getMethod("getCallerClass", Array(classOf[Int]): _*)
      Some((i: Int) ⇒ m.invoke(null, Array[AnyRef](i.asInstanceOf[java.lang.Integer]): _*).asInstanceOf[Class[_]])
    } catch {
      case NonFatal(e) ⇒ None
    }
  }

  def instantiate[T](clazz: Class[T], args: immutable.Seq[Any]): T = {
    instantiate(findConstructor(clazz, args), args)
  }

  def instantiate[T](constructor: Constructor[T], args: immutable.Seq[Any]): T = {
    constructor.setAccessible(true)
    try constructor.newInstance(args.asInstanceOf[Seq[AnyRef]]: _*)
    catch {
      case e: IllegalArgumentException ⇒
        val argString = args map safeGetClass mkString ("[", ", ", "]")
        throw new IllegalArgumentException(s"constructor $constructor is incompatible with arguments $argString", e)
    }
  }

  def findConstructor[T](clazz: Class[T], args: immutable.Seq[Any]): Constructor[T] = {
    def error(msg: String): Nothing = {
      val argClasses = args map safeGetClass mkString ", "
      throw new IllegalArgumentException(s"$msg found on $clazz for arguments [$argClasses]")
    }

    val constructor: Constructor[T] =
      if (args.isEmpty) Try {
        clazz.getDeclaredConstructor()
      } getOrElse null
      else {
        val length = args.length
        val candidates =
          clazz.getDeclaredConstructors.asInstanceOf[Array[Constructor[T]]].iterator filter { c ⇒
            val parameterTypes = c.getParameterTypes
            parameterTypes.length == length &&
              (parameterTypes.iterator zip args.iterator forall {
                case (found, required) ⇒
                  found.isInstance(required) || BoxedType(found).isInstance(required) ||
                    (required == null && !found.isPrimitive)
              })
          }
        if (candidates.hasNext) {
          val cstrtr = candidates.next()
          if (candidates.hasNext) error("multiple matching constructors")
          else cstrtr
        } else null
      }

    if (constructor == null) error("no matching constructor")
    else constructor
  }

  def instantiate[T](clazz: Class[T]): T = try clazz.newInstance catch {
    case iae: IllegalAccessException ⇒
      val ctor = clazz.getDeclaredConstructor()
      ctor.setAccessible(true)
      ctor.newInstance()
  }

  private def safeGetClass(a: Any): Class[_] =
    if (a == null) classOf[AnyRef] else a.getClass

  @varargs
  def subTypeOfJava[T](clazz: Class[T], classLoader: ClassLoader, packages: String*): java.util.Set[Class[_ <: T]] = {
    val configuration = {
      val builder = new ConfigurationBuilder
      for (packageName <- packages) {
        builder.addUrls(ClasspathHelper.forPackage(packageName, classLoader))
      }
      builder.addClassLoader(classLoader)
    }
    val reflections = new Reflections(configuration)
    reflections.getSubTypesOf(clazz)
  }

  @varargs
  def subTypeOf[T](clazz: Class[T], classLoader: ClassLoader, packages: String*): Set[Class[_ <: T]] = {
    import scala.collection.JavaConverters._
    subTypeOfJava(clazz, classLoader, packages: _*).asScala.toSet
  }

  def typeAnnotationWith(clazz: Class[_ <: Annotation], classLoader: ClassLoader, packages: String*): Set[Class[_]] = {
    val configuration = {
      val builder = new ConfigurationBuilder
      for (packageName <- packages) {
        builder.addUrls(ClasspathHelper.forPackage(packageName, classLoader))
      }
      builder.addClassLoader(classLoader)
    }
    val reflections = new Reflections(configuration)

    import scala.collection.JavaConversions._
    reflections.getTypesAnnotatedWith(clazz).toSet
  }

  def findSuperClassOf(clazz: Class[_]): Set[Class[_]] = {
    import scala.collection.JavaConversions._
    ReflectionUtils.getAllSuperTypes(clazz, new com.google.common.base.Predicate[T forSome { type T >: Class[_] }] {
      override def apply(input: (T) forSome { type T >: Class[_] }): Boolean = true
    }).toSet
  }

  //TODO think more about this,not useable
  private val typeParamterClassCache: Map[Class[_], Map[String, Class[_]]] = Map.empty.withDefault(Map.empty)
  def typeParameterClass(target: AnyRef, parameterizedSuperclass: Class[_], typeParamName: String): Class[_] = {
    val thisClass = target.getClass

    def findTypeParameterClass(tpn: String): Class[_] = {
      val allSuperParameterizedClazzes = findSuperClassOf(thisClass).filter(_.getGenericSuperclass.isInstanceOf[ParameterizedType])
      if (allSuperParameterizedClazzes.isEmpty) {
        classOf[AnyRef]
      }
      allSuperParameterizedClazzes.map(_.getGenericSuperclass).collectFirst {
        case t: ParameterizedType => t.getRawType
        case t: Class[_] => t
        case t: GenericArrayType =>
          t.getGenericComponentType match {
            case t2: ParameterizedType =>
              t2.getRawType
            case t2: Class[_] =>
              java.lang.reflect.Array.newInstance(t2.asInstanceOf[Class[_]], 0).getClass
          }
        case t: TypeVariable[_] =>
          t.getGenericDeclaration match {
            case c: Class[_] =>
              val ntpn = c.getName
              if (c.isAssignableFrom(thisClass)) {
                findTypeParameterClass(ntpn)
              } else {
                classOf[AnyRef]
              }
            case _ =>
              classOf[AnyRef]
          }
      }.map(_.asInstanceOf[Class[_]]).orNull
    }

    typeParamterClassCache.get(thisClass) match {
      case Some(v) =>
        v.get(typeParamName) match {
          case Some(t) =>
            t
          case None =>
            findTypeParameterClass(typeParamName)
        }
      case None =>
        findTypeParameterClass(typeParamName)
    }
  }

  object BoxedType {
    import java.{ lang => jl }

    private val toBoxed = Map[Class[_], Class[_]](
      classOf[Boolean] -> classOf[jl.Boolean],
      classOf[Byte] -> classOf[jl.Byte],
      classOf[Char] -> classOf[jl.Character],
      classOf[Short] -> classOf[jl.Short],
      classOf[Int] -> classOf[jl.Integer],
      classOf[Long] -> classOf[jl.Long],
      classOf[Float] -> classOf[jl.Float],
      classOf[Double] -> classOf[jl.Double],
      classOf[Unit] -> classOf[scala.runtime.BoxedUnit]
    )

    final def apply(c: Class[_]): Class[_] = if (c.isPrimitive) toBoxed(c) else c
  }
}


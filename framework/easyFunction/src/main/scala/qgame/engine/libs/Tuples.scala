package qgame.engine.libs

object Tuples {
  /**
   * Used to create tuples with 1 elements in Java.
   */
  object Tuple1 {
    def create[T1](t1: T1) = new Tuple1(t1)
  }

  /**
   * Java Tuple1 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple1[T1](t1: T1) {
    val toScala: scala.Tuple1[T1] = scala.Tuple1.apply(t1)
  }

  /**
   * Used to create tuples with 2 elements in Java.
   */
  object Tuple2 {
    def create[T1, T2](t1: T1, t2: T2) = new Tuple2(t1, t2)
  }

  /**
   * Java Tuple2 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple2[T1, T2](t1: T1, t2: T2) {
    val toScala: (T1, T2) = (t1, t2)
  }

  /**
   * Used to create tuples with 3 elements in Java.
   */
  object Tuple3 {
    def create[T1, T2, T3](t1: T1, t2: T2, t3: T3) = new Tuple3(t1, t2, t3)
  }

  /**
   * Java Tuple3 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple3[T1, T2, T3](t1: T1, t2: T2, t3: T3) {
    val toScala: (T1, T2, T3) = (t1, t2, t3)
  }

  /**
   * Used to create tuples with 4 elements in Java.
   */
  object Tuple4 {
    def create[T1, T2, T3, T4](t1: T1, t2: T2, t3: T3, t4: T4) = new Tuple4(t1, t2, t3, t4)
  }

  /**
   * Java Tuple4 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple4[T1, T2, T3, T4](t1: T1, t2: T2, t3: T3, t4: T4) {
    val toScala: (T1, T2, T3, T4) = (t1, t2, t3, t4)
  }

  /**
   * Used to create tuples with 5 elements in Java.
   */
  object Tuple5 {
    def create[T1, T2, T3, T4, T5](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5) = new Tuple5(t1, t2, t3, t4, t5)
  }

  /**
   * Java Tuple5 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple5[T1, T2, T3, T4, T5](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5) {
    val toScala: (T1, T2, T3, T4, T5) = (t1, t2, t3, t4, t5)
  }

  /**
   * Used to create tuples with 6 elements in Java.
   */
  object Tuple6 {
    def create[T1, T2, T3, T4, T5, T6](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6) = new Tuple6(t1, t2, t3, t4, t5, t6)
  }

  /**
   * Java Tuple6 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple6[T1, T2, T3, T4, T5, T6](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6) {
    val toScala: (T1, T2, T3, T4, T5, T6) = (t1, t2, t3, t4, t5, t6)
  }

  /**
   * Used to create tuples with 7 elements in Java.
   */
  object Tuple7 {
    def create[T1, T2, T3, T4, T5, T6, T7](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7) = new Tuple7(t1, t2, t3, t4, t5, t6, t7)
  }

  /**
   * Java Tuple7 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple7[T1, T2, T3, T4, T5, T6, T7](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7) = (t1, t2, t3, t4, t5, t6, t7)
  }

  /**
   * Used to create tuples with 8 elements in Java.
   */
  object Tuple8 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8) = new Tuple8(t1, t2, t3, t4, t5, t6, t7, t8)
  }

  /**
   * Java Tuple8 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple8[T1, T2, T3, T4, T5, T6, T7, T8](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8) = (t1, t2, t3, t4, t5, t6, t7, t8)
  }

  /**
   * Used to create tuples with 9 elements in Java.
   */
  object Tuple9 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9) = new Tuple9(t1, t2, t3, t4, t5, t6, t7, t8, t9)
  }

  /**
   * Java Tuple9 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple9[T1, T2, T3, T4, T5, T6, T7, T8, T9](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9) = (t1, t2, t3, t4, t5, t6, t7, t8, t9)
  }

  /**
   * Used to create tuples with 10 elements in Java.
   */
  object Tuple10 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10) = new Tuple10(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10)
  }

  /**
   * Java Tuple10 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10)
  }

  /**
   * Used to create tuples with 11 elements in Java.
   */
  object Tuple11 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11) = new Tuple11(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)
  }

  /**
   * Java Tuple11 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)
  }

  /**
   * Used to create tuples with 12 elements in Java.
   */
  object Tuple12 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12) = new Tuple12(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12)
  }

  /**
   * Java Tuple12 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12)
  }

  /**
   * Used to create tuples with 13 elements in Java.
   */
  object Tuple13 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13) = new Tuple13(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13)
  }

  /**
   * Java Tuple13 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13)
  }

  /**
   * Used to create tuples with 14 elements in Java.
   */
  object Tuple14 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14) = new Tuple14(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14)
  }

  /**
   * Java Tuple14 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14)
  }

  /**
   * Used to create tuples with 15 elements in Java.
   */
  object Tuple15 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15) = new Tuple15(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15)
  }

  /**
   * Java Tuple15 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15)
  }

  /**
   * Used to create tuples with 16 elements in Java.
   */
  object Tuple16 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16) = new Tuple16(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16)
  }

  /**
   * Java Tuple16 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16)
  }

  /**
   * Used to create tuples with 17 elements in Java.
   */
  object Tuple17 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17) = new Tuple17(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17)
  }

  /**
   * Java Tuple17 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17)
  }

  /**
   * Used to create tuples with 18 elements in Java.
   */
  object Tuple18 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18) = new Tuple18(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18)
  }

  /**
   * Java Tuple18 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18)
  }

  /**
   * Used to create tuples with 19 elements in Java.
   */
  object Tuple19 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19) = new Tuple19(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19)
  }

  /**
   * Java Tuple19 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19)
  }

  /**
   * Used to create tuples with 20 elements in Java.
   */
  object Tuple20 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20) = new Tuple20(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20)
  }

  /**
   * Java Tuple20 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20)
  }

  /**
   * Used to create tuples with 21 elements in Java.
   */
  object Tuple21 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20, t21: T21) = new Tuple21(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21)
  }

  /**
   * Java Tuple21 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20, t21: T21) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21)
  }

  /**
   * Used to create tuples with 22 elements in Java.
   */
  object Tuple22 {
    def create[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20, t21: T21, t22: T22) = new Tuple22(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22)
  }

  /**
   * Java Tuple22 container.
   */
  @SerialVersionUID(1L)
  final case class Tuple22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20, t21: T21, t22: T22) {
    val toScala: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22) = (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22)
  }
}
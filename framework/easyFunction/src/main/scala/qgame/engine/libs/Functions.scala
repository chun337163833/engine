package qgame.engine.libs

/**
 * Created by kerr.
 */
object Functions {
  /**
   * A Function interface. Used to create 1-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function1[-T1, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1): R
  }

  /**
   * A Function interface. Used to create 2-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function2[-T1, -T2, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2): R
  }

  /**
   * A Function interface. Used to create 3-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function3[-T1, -T2, -T3, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3): R
  }

  /**
   * A Function interface. Used to create 4-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function4[-T1, -T2, -T3, -T4, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4): R
  }

  /**
   * A Function interface. Used to create 5-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function5[-T1, -T2, -T3, -T4, -T5, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5): R
  }

  /**
   * A Function interface. Used to create 6-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function6[-T1, -T2, -T3, -T4, -T5, -T6, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6): R
  }

  /**
   * A Function interface. Used to create 7-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function7[-T1, -T2, -T3, -T4, -T5, -T6, -T7, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7): R
  }

  /**
   * A Function interface. Used to create 8-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function8[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8): R
  }

  /**
   * A Function interface. Used to create 9-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function9[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9): R
  }

  /**
   * A Function interface. Used to create 10-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function10[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10): R
  }

  /**
   * A Function interface. Used to create 11-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function11[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11): R
  }

  /**
   * A Function interface. Used to create 12-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function12[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12): R
  }

  /**
   * A Function interface. Used to create 13-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function13[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13): R
  }

  /**
   * A Function interface. Used to create 14-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function14[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14): R
  }

  /**
   * A Function interface. Used to create 15-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function15[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15): R
  }

  /**
   * A Function interface. Used to create 16-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function16[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16): R
  }

  /**
   * A Function interface. Used to create 17-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function17[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17): R
  }

  /**
   * A Function interface. Used to create 18-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function18[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18): R
  }

  /**
   * A Function interface. Used to create 19-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function19[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19): R
  }

  /**
   * A Function interface. Used to create 20-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function20[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20): R
  }

  /**
   * A Function interface. Used to create 21-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function21[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21): R
  }

  /**
   * A Function interface. Used to create 22-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function22[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22): R
  }

  /**
   * A Function interface. Used to create 23-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function23[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23): R
  }

  /**
   * A Function interface. Used to create 24-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function24[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24): R
  }

  /**
   * A Function interface. Used to create 25-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function25[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25): R
  }

  /**
   * A Function interface. Used to create 26-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function26[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26): R
  }

  /**
   * A Function interface. Used to create 27-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function27[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, -T27, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26, arg27: T27): R
  }

  /**
   * A Function interface. Used to create 28-arg first-class-functions is Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Function28[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, -T27, -T28, +R] extends java.io.Serializable {
    @throws(classOf[Exception])
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26, arg27: T27, arg28: T28): R
  }

  /**
   * A Consumer interface. Used to create 1-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer1[-T1] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1): Unit
  }

  /**
   * A Consumer interface. Used to create 2-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer2[-T1, -T2] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2): Unit
  }

  /**
   * A Consumer interface. Used to create 3-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer3[-T1, -T2, -T3] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3): Unit
  }

  /**
   * A Consumer interface. Used to create 4-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer4[-T1, -T2, -T3, -T4] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4): Unit
  }

  /**
   * A Consumer interface. Used to create 5-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer5[-T1, -T2, -T3, -T4, -T5] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5): Unit
  }

  /**
   * A Consumer interface. Used to create 6-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer6[-T1, -T2, -T3, -T4, -T5, -T6] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6): Unit
  }

  /**
   * A Consumer interface. Used to create 7-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer7[-T1, -T2, -T3, -T4, -T5, -T6, -T7] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7): Unit
  }

  /**
   * A Consumer interface. Used to create 8-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer8[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8): Unit
  }

  /**
   * A Consumer interface. Used to create 9-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer9[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9): Unit
  }

  /**
   * A Consumer interface. Used to create 10-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer10[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10): Unit
  }

  /**
   * A Consumer interface. Used to create 11-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer11[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11): Unit
  }

  /**
   * A Consumer interface. Used to create 12-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer12[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12): Unit
  }

  /**
   * A Consumer interface. Used to create 13-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer13[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13): Unit
  }

  /**
   * A Consumer interface. Used to create 14-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer14[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14): Unit
  }

  /**
   * A Consumer interface. Used to create 15-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer15[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15): Unit
  }

  /**
   * A Consumer interface. Used to create 16-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer16[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16): Unit
  }

  /**
   * A Consumer interface. Used to create 17-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer17[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17): Unit
  }

  /**
   * A Consumer interface. Used to create 18-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer18[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18): Unit
  }

  /**
   * A Consumer interface. Used to create 19-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer19[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19): Unit
  }

  /**
   * A Consumer interface. Used to create 20-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer20[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20): Unit
  }

  /**
   * A Consumer interface. Used to create 21-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer21[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21): Unit
  }

  /**
   * A Consumer interface. Used to create 22-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer22[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22): Unit
  }

  /**
   * A Consumer interface. Used to create 23-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer23[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23): Unit
  }

  /**
   * A Consumer interface. Used to create 24-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer24[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24): Unit
  }

  /**
   * A Consumer interface. Used to create 25-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer25[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25): Unit
  }

  /**
   * A Consumer interface. Used to create 26-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer26[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26): Unit
  }

  /**
   * A Consumer interface. Used to create 27-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer27[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, -T27] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26, arg27: T27): Unit
  }

  /**
   * A Consumer interface. Used to create 28-arg consumers in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Consumer28[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, -T27, -T28] extends java.io.Serializable {
    @throws(classOf[Exception])
    def accept(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26, arg27: T27, arg28: T28): Unit
  }

  /**
   * A Predicate interface. Used to create 1-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate1[-T1] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1): Boolean
  }

  /**
   * A Predicate interface. Used to create 2-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate2[-T1, -T2] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2): Boolean
  }

  /**
   * A Predicate interface. Used to create 3-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate3[-T1, -T2, -T3] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3): Boolean
  }

  /**
   * A Predicate interface. Used to create 4-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate4[-T1, -T2, -T3, -T4] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4): Boolean
  }

  /**
   * A Predicate interface. Used to create 5-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate5[-T1, -T2, -T3, -T4, -T5] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5): Boolean
  }

  /**
   * A Predicate interface. Used to create 6-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate6[-T1, -T2, -T3, -T4, -T5, -T6] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6): Boolean
  }

  /**
   * A Predicate interface. Used to create 7-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate7[-T1, -T2, -T3, -T4, -T5, -T6, -T7] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7): Boolean
  }

  /**
   * A Predicate interface. Used to create 8-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate8[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8): Boolean
  }

  /**
   * A Predicate interface. Used to create 9-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate9[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9): Boolean
  }

  /**
   * A Predicate interface. Used to create 10-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate10[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10): Boolean
  }

  /**
   * A Predicate interface. Used to create 11-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate11[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11): Boolean
  }

  /**
   * A Predicate interface. Used to create 12-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate12[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12): Boolean
  }

  /**
   * A Predicate interface. Used to create 13-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate13[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13): Boolean
  }

  /**
   * A Predicate interface. Used to create 14-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate14[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14): Boolean
  }

  /**
   * A Predicate interface. Used to create 15-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate15[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15): Boolean
  }

  /**
   * A Predicate interface. Used to create 16-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate16[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16): Boolean
  }

  /**
   * A Predicate interface. Used to create 17-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate17[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17): Boolean
  }

  /**
   * A Predicate interface. Used to create 18-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate18[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18): Boolean
  }

  /**
   * A Predicate interface. Used to create 19-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate19[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19): Boolean
  }

  /**
   * A Predicate interface. Used to create 20-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate20[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20): Boolean
  }

  /**
   * A Predicate interface. Used to create 21-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate21[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21): Boolean
  }

  /**
   * A Predicate interface. Used to create 22-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate22[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22): Boolean
  }

  /**
   * A Predicate interface. Used to create 23-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate23[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23): Boolean
  }

  /**
   * A Predicate interface. Used to create 24-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate24[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24): Boolean
  }

  /**
   * A Predicate interface. Used to create 25-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate25[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25): Boolean
  }

  /**
   * A Predicate interface. Used to create 26-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate26[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26): Boolean
  }

  /**
   * A Predicate interface. Used to create 27-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate27[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, -T27] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26, arg27: T27): Boolean
  }

  /**
   * A Predicate interface. Used to create 28-arg predicates in Java.
   */
  @SerialVersionUID(1L)
  @FunctionalInterface
  trait Predicate28[-T1, -T2, -T3, -T4, -T5, -T6, -T7, -T8, -T9, -T10, -T11, -T12, -T13, -T14, -T15, -T16, -T17, -T18, -T19, -T20, -T21, -T22, -T23, -T24, -T25, -T26, -T27, -T28] extends java.io.Serializable {
    @throws(classOf[Exception])
    def test(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8, arg9: T9, arg10: T10, arg11: T11, arg12: T12, arg13: T13, arg14: T14, arg15: T15, arg16: T16, arg17: T17, arg18: T18, arg19: T19, arg20: T20, arg21: T21, arg22: T22, arg23: T23, arg24: T24, arg25: T25, arg26: T26, arg27: T27, arg28: T28): Boolean
  }
}

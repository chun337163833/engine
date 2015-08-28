package qgame.engine.libs

import java.io._
import java.nio.file.FileVisitOption._
import java.nio.file.FileVisitResult._
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.{ ATOMIC_MOVE, REPLACE_EXISTING }
import java.nio.file._
import java.nio.file.attribute.{ BasicFileAttributes, PosixFileAttributeView, PosixFilePermissions }
import java.util
import java.util.Date
import java.util.regex.Pattern

import qgame.engine.libs.Functions.{ Consumer1, Predicate1 }

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

/**
 * Created by kerr.
 */
object FileSystem {
  //copy file
  def copy(from: String, to: String): FileSystem.type = {
    copy(from, to, replace = false)
  }

  def copy(from: InputStream, to: String): FileSystem.type = {
    Files.copy(from, Paths.get(to), StandardCopyOption.REPLACE_EXISTING)
    this
  }

  def copy(from: String, to: String, replace: Boolean): FileSystem.type = {
    val fromFile = new File(from)
    val toPath = Paths.get(to)
    val fromPath = fromFile.toPath
    if (!fromFile.exists()) {
      throw new IllegalArgumentException(s"file not exist $from")
    }
    if (fromFile.isDirectory) {
      //if is directory,copy recursive
      Files.walkFileTree(fromFile.toPath, util.EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, {
        val visitor = new SimpleFileVisitor[Path]() {
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            val target = toPath.resolve(fromPath.relativize(dir))
            try {
              if (replace) {
                Files.copy(dir, target, REPLACE_EXISTING)
              } else {
                Files.copy(dir, target)
              }
            } catch {
              case e: FileAlreadyExistsException =>
                if (!Files.isDirectory(target)) {
                  throw e
                }
            }
            CONTINUE
          }

          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            val target = toPath.resolve(fromPath.relativize(file))
            if (replace) {
              Files.copy(file, target, REPLACE_EXISTING)
            } else {
              Files.copy(file, target)
            }
            CONTINUE
          }
        }
        visitor
      })
    } else {
      if (replace) {
        Files.copy(fromFile.toPath, toPath, REPLACE_EXISTING)
      } else {
        Files.copy(fromFile.toPath, toPath)
      }
    }
    this
  }

  def move(from: String, to: String): FileSystem.type = {
    move(from, to, atomic = false)
  }

  def move(from: String, to: String, atomic: Boolean): FileSystem.type = {
    val fromFile = new File(from)
    val toPath = Paths.get(to)
    val fromPath = fromFile.toPath
    if (!fromFile.exists()) {
      throw new IllegalArgumentException(s"file not exist $from")
    }
    if (atomic) {
      Files.move(fromPath, toPath, REPLACE_EXISTING, ATOMIC_MOVE)
    } else {
      Files.move(fromPath, toPath, REPLACE_EXISTING)
    }
    this
  }

  def hardlink(link: String, existing: String): FileSystem.type = {
    val targetPath = Paths.get(existing)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $existing")
    }
    val linkPath = Paths.get(link)
    Files.createLink(linkPath, targetPath)
    this
  }

  def symbolink(link: String, existing: String): FileSystem.type = {
    val targetPath = Paths.get(existing)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $existing")
    }
    val linkPath = Paths.get(link)
    Files.createSymbolicLink(linkPath, targetPath)
    this
  }

  def linkProps(path: String): FileProps = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    val attributes = Files.readAttributes(targetPath, classOf[BasicFileAttributes], NOFOLLOW_LINKS)
    attributes
  }

  private implicit def Attributes2FileProps(attr: BasicFileAttributes): FileProps = {
    FileProps(
      creationTime = new Date(attr.creationTime().toMillis),
      lastAccessTime = new Date(attr.lastAccessTime().toMillis),
      lastModifiedTime = new Date(attr.lastModifiedTime().toMillis),
      attr.isDirectory,
      attr.isOther,
      attr.isRegularFile,
      attr.isSymbolicLink,
      attr.size()
    )
  }

  def chown(path: String, user: String, group: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    val service = targetPath.getFileSystem.getUserPrincipalLookupService
    val userPrinciple = if (user eq null) null else service.lookupPrincipalByName(user)
    val groupPrinciple = if (group eq null) null else service.lookupPrincipalByGroupName(group)
    if (groupPrinciple ne null) {
      val view = Files.getFileAttributeView(targetPath, classOf[PosixFileAttributeView], NOFOLLOW_LINKS)
      if (view eq null) {
        throw new IllegalArgumentException(s"could not change group")
      }
      view.setGroup(groupPrinciple)
    }
    if (userPrinciple ne null) {
      Files.setOwner(targetPath, userPrinciple)
    }
    this
  }

  def chmod(path: String, permission: String): FileSystem.type = {
    chmod(path, permission, null)
  }

  def chmod(path: String, permission: String, dirPermission: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    val filePermission = PosixFilePermissions.fromString(permission)
    val dirPermissions = if (dirPermission eq null) null else PosixFilePermissions.fromString(dirPermission)
    if (dirPermission eq null) {
      Files.setPosixFilePermissions(targetPath, filePermission)
    } else {
      Files.walkFileTree(targetPath, {
        val fileVisitor = new SimpleFileVisitor[Path] {
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.setPosixFilePermissions(dir, dirPermissions)
            CONTINUE
          }

          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.setPosixFilePermissions(file, filePermission)
            CONTINUE
          }
        }
        fileVisitor
      })
    }
    this
  }

  def props(path: String): FileSystemProps = {
    val targetPath = Paths.get(path)
    if (Files.exists(targetPath)) {
      throw new IllegalArgumentException(s"target file not exist $path")
    }
    val fileStore = Files.getFileStore(targetPath)
    fileStore
  }
  implicit def FileStore2FileSystemProps(fileStore: FileStore): FileSystemProps = {
    FileSystemProps(
      totalSpace = fileStore.getTotalSpace,
      unallocatedSpace = fileStore.getUnallocatedSpace,
      usableSpace = fileStore.getUsableSpace
    )
  }

  def truncate(path: String, length: Long): FileSystem.type = {
    val targetPath = Paths.get(path)
    if (Files.exists(targetPath)) {
      throw new IllegalArgumentException(s"target file not exist $path")
    }
    if (length < 0) {
      throw new IllegalArgumentException(s"length can not < 0 $length")
    }
    try {
      val file = new RandomAccessFile(path, "rw")
      try {
        file.setLength(length)
      } finally {
        file.close()
      }
    } catch {
      case e: Exception => throw e
    }
    this
  }

  def delete(path: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    if (Files.exists(targetPath)) {
      throw new IllegalArgumentException(s"target file not exist $path")
    }
    if (Files.isDirectory(targetPath)) {
      Files.walkFileTree(targetPath, {
        val fileVisitor = new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.delete(file)
            CONTINUE
          }

          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            if (exc eq null) {
              Files.delete(dir)
              CONTINUE
            } else {
              throw exc
            }
          }
        }
        fileVisitor
      })
    } else {
      Files.delete(targetPath)
    }
    this
  }

  def write(path: String, bytes: Array[Byte]): FileSystem.type = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    Files.write(targetPath, bytes)
    this
  }

  def readDir(path: String): Array[File] = {
    val targetFile = new File(path)
    if (!targetFile.exists()) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    if (!targetFile.isDirectory) {
      throw new IllegalArgumentException(s"$path is not a directory")
    }
    targetFile.listFiles()
  }

  def readDir(path: String, filter: String): Array[File] = {
    val targetFile = new File(path)
    if (!targetFile.exists()) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    if (!targetFile.isDirectory) {
      throw new IllegalArgumentException(s"$path is not a directory")
    }
    val allFiles = if (filter eq null) {
      targetFile.listFiles()
    } else {
      targetFile.listFiles(new FilenameFilter {
        def accept(dir: File, name: String): Boolean = {
          Pattern.matches(filter, name)
        }
      })
    }
    allFiles
  }

  def readDirTree(path: String, directoryFilter: (Path) => Boolean, fileFilter: (Path) => Boolean): Array[File] = {
    val buffer = new ArrayBuffer[File]
    handleDirTree(path, directoryFilter, fileFilter) {
      file =>
        buffer += file
    }
    buffer.toArray
  }

  def readDirTree(path: String, directoryFilter: Predicate1[Path], fileFilter: Predicate1[Path]): Array[File] = {
    readDirTree(path, directoryFilter.test _, fileFilter.test _)
  }

  def readDirTree(path: String, filter: String): Array[File] = {
    val buffer = new ArrayBuffer[File]
    handleDirTree(path, _ => true, _ => true) {
      file =>
        if (Pattern.matches(filter, file.getName)) {
          buffer += file
        }
    }
    buffer.toArray
  }

  def read(path: String): Array[Byte] = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    Files.readAllBytes(targetPath)
  }

  def readSymbolink(link: String): String = {
    val targetPath = Paths.get(link)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $link")
    }
    Files.readSymbolicLink(targetPath).toString
  }

  def fileProps(path: String): FileProps = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    val attributes = Files.readAttributes(targetPath, classOf[BasicFileAttributes])
    attributes
  }

  def mkdir(path: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    Files.createDirectories(targetPath)
    this
  }

  def mkdirTree(base: String, sub: String*): FileSystem.type = {
    val targetPath = Paths.get(base, sub: _*)
    Files.createDirectories(targetPath)
    this
  }

  def mkdir(path: String, permission: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    val attribute = if (permission eq null) null else PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(permission))
    Files.createDirectories(targetPath, attribute)
    this
  }

  def create(path: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    Files.createFile(targetPath)
    this
  }

  def create(path: String, permission: String): FileSystem.type = {
    val targetPath = Paths.get(path)
    val attribute = if (permission eq null) null else PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(permission))
    Files.createFile(targetPath, attribute)
    this
  }

  def exists(path: String): Boolean = {
    Files.exists(Paths.get(path))
  }

  def exists(base: String, path: String): Boolean = {
    Files.exists(Paths.get(base, path))
  }

  def handleDirTree(path: String, directoryFilter: Path => Boolean, fileFilter: Path => Boolean)(f: (File) => Unit): FileSystem.type = {
    val targetPath = Paths.get(path)
    if (Files.notExists(targetPath)) {
      throw new IllegalArgumentException(s"file not exist $path")
    }
    if (!Files.isDirectory(targetPath)) {
      throw new IllegalArgumentException(s"$path is not a directory")
    } else {
      if (f eq null) {
        throw new IllegalArgumentException(s"function can not be null")
      }
      Files.walkFileTree(targetPath, {
        val fileVisitor = new SimpleFileVisitor[Path] {

          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            if (fileFilter(file)) {
              f(file.toFile)
            } else {
              //NOOP
            }
            CONTINUE
          }

          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            //do the filter here
            if (directoryFilter(dir)) {
              CONTINUE
            } else {
              SKIP_SUBTREE
            }
          }
        }
        fileVisitor
      })
    }
    this
  }

  def handleDirTree(path: String, handler: Consumer1[File]): FileSystem.type = {
    handleDirTree(path, _ => true, _ => true)(handler.accept)
  }
}

case class FileSystemProps(
  totalSpace: Long,
  unallocatedSpace: Long,
  usableSpace: Long
)

case class FileProps(
  creationTime: Date,
  lastAccessTime: Date,
  lastModifiedTime: Date,
  isDirectory: Boolean,
  isOther: Boolean,
  isRegularFile: Boolean,
  isSymbolicLink: Boolean,
  size: Long
)

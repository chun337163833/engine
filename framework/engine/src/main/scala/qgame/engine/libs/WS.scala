package qgame.engine.libs

import java.io.{ File, InputStream }
import java.util
import java.util.concurrent.Executors

import com.ning.http.client._
import com.ning.http.client.cookie.Cookie
import com.ning.http.client.multipart.Part
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
//TODO split this out
object WS extends WSAPI {
  private val client: AsyncHttpClient = {
    val configBuilder = new AsyncHttpClientConfig.Builder()
      .setPooledConnectionIdleTimeout(12 * 1000)
      .setConnectionTTL(10 * 1000)
      .setConnectTimeout(5 * 1000)
      .setRequestTimeout(6 * 1000)
      .setMaxRequestRetry(5)
      .setFollowRedirect(true)
      .setAllowPoolingConnections(false)
      .setAllowPoolingSslConnections(false)
      .setCompressionEnforced(true)
    val nettyAsyncHttpProviderConfig = new NettyAsyncHttpProviderConfig
    nettyAsyncHttpProviderConfig.setBossExecutorService(Executors.newCachedThreadPool())
    configBuilder.setAsyncHttpClientProviderConfig(nettyAsyncHttpProviderConfig)
    new AsyncHttpClient(configBuilder.build())
  }

  override def prepareGet(url: String): WSBuilder = WSBuilder(client).prepareGet(url)

  override def preparePut(url: String): WSBuilder = WSBuilder(client).preparePut(url)

  override def preparePost(url: String): WSBuilder = WSBuilder(client).preparePost(url)

  override def prepareOptions(url: String): WSBuilder = WSBuilder(client).prepareOptions(url)

  override def prepareHead(url: String): WSBuilder = WSBuilder(client).prepareHead(url)

  override def prepareConnect(url: String): WSBuilder = WSBuilder(client).prepareConnect(url)

  override def prepareDelete(url: String): WSBuilder = WSBuilder(client).prepareDelete(url)
}

trait WSAPI {
  def prepareGet(url: String): WSBuilder

  def prepareConnect(url: String): WSBuilder

  def prepareOptions(url: String): WSBuilder

  def prepareHead(url: String): WSBuilder

  def preparePost(url: String): WSBuilder

  def preparePut(url: String): WSBuilder

  def prepareDelete(url: String): WSBuilder
}

case class WSBuilder(client: AsyncHttpClient) {

  private var builderBase: AsyncHttpClient#BoundRequestBuilder = null

  def prepareGet(url: String): WSBuilder = {
    builderBase = client.prepareGet(url)
    this
  }

  def prepareConnect(url: String): WSBuilder = {
    builderBase = client.prepareConnect(url)
    this
  }

  def prepareOptions(url: String): WSBuilder = {
    builderBase = client.prepareOptions(url)
    this
  }

  def prepareHead(url: String): WSBuilder = {
    builderBase = client.prepareHead(url)
    this
  }

  def preparePost(url: String): WSBuilder = {
    builderBase = client.preparePost(url)
    this
  }

  def preparePut(url: String): WSBuilder = {
    builderBase = client.preparePut(url)
    this
  }

  def prepareDelete(url: String): WSBuilder = {
    builderBase = client.prepareDelete(url)
    this
  }

  def setProxyServer(proxyServer: ProxyServer): WSBuilder = {
    builderBase.setProxyServer(proxyServer)
    this
  }

  def addBodyPart(part: Part): WSBuilder = {
    builderBase.addBodyPart(part)
    this
  }

  def addCookie(cookie: Cookie): WSBuilder = {
    builderBase.addCookie(cookie)
    this
  }

  def addHeader(name: String, value: String): WSBuilder = {
    builderBase.addHeader(name, value)
    this
  }

  def addFormParam(key: String, value: String): WSBuilder = {
    builderBase.addFormParam(key, value)
    this
  }

  def addQueryParam(name: String, value: String): WSBuilder = {
    builderBase.addQueryParam(name, value)
    this
  }

  def setBody(data: Array[Byte]): WSBuilder = {
    builderBase.setBody(data)
    this
  }

  def setBody(stream: InputStream): WSBuilder = {
    builderBase.setBody(stream)
    this
  }

  def setBody(data: String): WSBuilder = {
    builderBase.setBody(data)
    this
  }

  def setBody(data: BodyGenerator): WSBuilder = {
    builderBase.setBody(data)
    this
  }

  def setBody(data: util.List[Array[Byte]]): WSBuilder = {
    builderBase.setBody(data)
    this
  }

  def setBody(data: File): WSBuilder = {
    builderBase.setBody(data)
    this
  }

  def setBodyEncoding(charset: String): WSBuilder = {
    builderBase.setBodyEncoding(charset)
    this
  }

  def setHeader(name: String, value: String): WSBuilder = {
    builderBase.setHeader(name, value)
    this
  }

  def setHeaders(headers: FluentCaseInsensitiveStringsMap): WSBuilder = {
    builderBase.setHeaders(headers)
    this
  }

  def setHeaders(headers: java.util.Map[String, util.Collection[String]]): WSBuilder = {
    builderBase.setHeaders(headers)
    this
  }

  def setQueryParams(parameters: java.util.Map[String, util.List[String]]): WSBuilder = {
    builderBase.setQueryParams(parameters)
    this
  }

  def setQueryParams(parameters: FluentStringsMap): WSBuilder = {
    builderBase.setQueryParams(parameters)
    this
  }

  def setFormParams(parameters: java.util.Map[String, util.List[String]]): WSBuilder = {
    builderBase.setFormParams(parameters)
    this
  }

  def setFormParams(parameters: FluentStringsMap): WSBuilder = {
    builderBase.setFormParams(parameters)
    this
  }

  def setVirtualHost(virtualHost: String): WSBuilder = {
    builderBase.setVirtualHost(virtualHost)
    this
  }

  def setSignatureCalculator(signatureCalculator: SignatureCalculator): WSBuilder = {
    builderBase.setSignatureCalculator(signatureCalculator)
    this
  }

  def execute(): Future[Response] = {
    val responsePromise = Promise[Response]()
    try {
      client.executeRequest(builderBase.build(), new AsyncCompletionHandler[Response]() {
        override def onCompleted(response: Response): Response = {
          responsePromise.success(response)
          response
        }

        override def onThrowable(t: Throwable): Unit = {
          responsePromise.failure(t)
        }
      })
      responsePromise.future
    } catch {
      case e: Exception =>
        responsePromise.failure(e)
    }
    responsePromise.future
  }

  def execute(handler: WSHandler): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    execute().onComplete {
      case Failure(e) =>
        handler.onFailure(e)
      case Success(response) =>
        handler.onSuccess(response)
    }
  }

}

abstract class SimpleWSHandler extends WSHandler {
  override def onFailure(e: Throwable): Unit = {
    e.printStackTrace()
  }
}

trait WSHandler {
  def onSuccess(response: Response)

  def onFailure(e: Throwable)
}

package server

import gust.backend.runtime.Logging
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import javax.inject.Singleton


/**
 * Observes for a [ServerBuilder] to be created in the bean context, and when it is, it mounts the built-in
 * [ProtoReflectionService] so that we may use the gRPC CLI and other tools.
 */
@Singleton
class ReflectionService: BeanCreatedEventListener<ServerBuilder<*>> {
  private val logging = Logging.logger(ReflectionService::class.java)

  override fun onCreated(event: BeanCreatedEvent<ServerBuilder<*>>): ServerBuilder<*> {
    // skip `AppService`/`CovidMapInterceptor` - they are installed by default
    // because they are members of the bean context.
    logging.info("Mounting gRPC reflection service...")
    return event.bean.addService(ProtoReflectionService.newInstance())
  }
}

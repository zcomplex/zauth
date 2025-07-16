package xauth.core.domain.system.port

import zio.Task

/** Service to handle service and its initialization. */
trait SystemService:

  /** Initializes the system and returns a task that holds true on success. */
  def init: Task[Boolean]
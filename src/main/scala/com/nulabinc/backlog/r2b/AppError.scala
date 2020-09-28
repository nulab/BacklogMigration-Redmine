package com.nulabinc.backlog.r2b

import com.nulabinc.backlog.migration.common.errors.MappingFileError

sealed trait AppError

case class ValidationError(errors: Seq[String])  extends AppError
case class MappingError(inner: MappingFileError) extends AppError
case object OperationCanceled                    extends AppError

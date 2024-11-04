package rvspeccore.core.spec

import rvspeccore.core.BaseCore
import instset._

trait RVInstSet
    extends BaseCore
    with IBase
    with MExtension
    with CExtension
    with ZifenceiExtension
    with ZicsrExtension
    with csr.CSRSupport
    with csr.ExceptionSupport
    with PrivilegedExtension
    with BExtension

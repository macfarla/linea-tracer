/*
 * Copyright ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.consensys.linea.zktracer.module.hub.section.call.precompileSubsection;

import static com.google.common.base.Preconditions.*;
import static net.consensys.linea.zktracer.module.hub.fragment.imc.mmu.MmuCall.callDataExtractionForIdentity;
import static net.consensys.linea.zktracer.module.hub.fragment.imc.mmu.MmuCall.partialCopyOfReturnDataForIdentity;
import static net.consensys.linea.zktracer.module.hub.fragment.imc.oob.OobInstruction.OOB_INST_IDENTITY;
import static net.consensys.linea.zktracer.module.hub.fragment.scenario.PrecompileScenarioFragment.PrecompileScenario.PRC_FAILURE_KNOWN_TO_HUB;

import net.consensys.linea.zktracer.module.hub.Hub;
import net.consensys.linea.zktracer.module.hub.fragment.imc.ImcFragment;
import net.consensys.linea.zktracer.module.hub.fragment.imc.mmu.MmuCall;
import net.consensys.linea.zktracer.module.hub.fragment.imc.oob.precompiles.PrecompileCommonOobCall;
import net.consensys.linea.zktracer.module.hub.section.call.CallSection;
import net.consensys.linea.zktracer.runtime.callstack.CallFrame;

public class IdentitySubsection extends PrecompileSubsection {

  final PrecompileCommonOobCall oobCall;

  public IdentitySubsection(final Hub hub, final CallSection callSection) {
    super(hub, callSection);

    final long calleeGas = callSection.stpCall.effectiveChildContextGasAllowance();
    oobCall = new PrecompileCommonOobCall(OOB_INST_IDENTITY, calleeGas);
    firstImcFragment.callOob(oobCall);

    if (!oobCall.isHubSuccess()) {
      precompileScenarioFragment.scenario(PRC_FAILURE_KNOWN_TO_HUB);
    }
  }

  @Override
  public void resolveAtContextReEntry(Hub hub, CallFrame callFrame) {
    super.resolveAtContextReEntry(hub, callFrame);

    // sanity check
    checkArgument(callSuccess == oobCall.isHubSuccess());

    if (!callSuccess) {
      precompileScenarioFragment.scenario(PRC_FAILURE_KNOWN_TO_HUB);
    }

    final boolean extractCallData = callSuccess && !getCallDataRange().isEmpty();
    if (extractCallData) {
      final MmuCall mmuCall = callDataExtractionForIdentity(hub, this);
      firstImcFragment.callMmu(mmuCall);
    }

    final ImcFragment secondImcFragment = ImcFragment.empty(hub);
    fragments().add(secondImcFragment);
    if (extractCallData && !getReturnAtRange().isEmpty()) {
      final MmuCall mmuCall = partialCopyOfReturnDataForIdentity(hub, this);
      secondImcFragment.callMmu(mmuCall);
    }
  }

  // 3 = 1 + 2 (scenario row + up to 2 miscellaneous fragments)
  @Override
  protected short maxNumberOfLines() {
    return 3;
    // Note: we don't have the successBit available at the moment
    // and can't provide the "real" value (2 in case of FKTH.)
  }
}

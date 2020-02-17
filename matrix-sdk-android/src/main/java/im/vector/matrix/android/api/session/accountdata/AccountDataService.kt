/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.accountdata

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataEvent

interface AccountDataService {
    /**
     * Retrieve the account data with the provided type or null if not found
     */
    fun getAccountDataEvent(type: String): UserAccountDataEvent?

    /**
     * Observe the account data with the provided type
     */
    fun getLiveAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>>

    /**
     * Retrieve the account data with the provided types. The return list can have a different size that
     * the size of the types set, because some AccountData may not exist.
     * If an empty set is provided, all the AccountData are retrieved
     */
    fun getAccountDataEvents(types: Set<String>): List<UserAccountDataEvent>

    /**
     * Observe the account data with the provided types. If an empty set is provided, all the AccountData are observed
     */
    fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>>

    /**
     * Update the account data with the provided type and the provided account data content
     */
    fun updateAccountData(type: String, content: Content, callback: MatrixCallback<Unit>? = null): Cancelable
}

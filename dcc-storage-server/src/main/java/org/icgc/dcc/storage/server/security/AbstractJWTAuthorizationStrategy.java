/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.storage.server.security;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.storage.server.jwt.JWTUser;
import org.icgc.dcc.storage.server.metadata.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public abstract class AbstractJWTAuthorizationStrategy {

    protected AuthScope scope;

    @Autowired
    protected MetadataService metadataService;

    /**
     * Package-private constructor for use in unit tests (to initialize scope context) without a Spring Context
     */
    protected AbstractJWTAuthorizationStrategy(final String scopeStr) {
        setAuthorizeScope(scopeStr);
        scope = AuthScope.from(getAuthorizeScope());
    }

    protected abstract void setAuthorizeScope(String scopeStr);

    protected abstract String getAuthorizeScope();

    protected abstract boolean verify(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId);

    /**
     * Main entry point: version of authorize() method that assumes object id is passed-in (method-level security has
     * access to this)
     */
    public boolean authorize(@NonNull Authentication authentication, @NonNull final String objectId) {
        log.info("Checking authorization with object id {}", objectId);
        scope = AuthScope.from(getAuthorizeScope());

        List<AuthScope> grantedScopes = Collections.<AuthScope> emptyList();

        if (authentication instanceof OAuth2Authentication) {
            val details = (OAuth2AuthenticationDetails) authentication.getDetails();
            grantedScopes = getScopes(details);
        }

        return verify(grantedScopes, objectId);
    }

    /**
     * Handles OAuth2Authentication object
     * @param authDetails from JWT
     * @return collection of AuthScope objects
     */
    protected List<AuthScope> getScopes(@NonNull OAuth2AuthenticationDetails authDetails) {
        val user = (JWTUser) authDetails.getDecodedDetails();
        val roles = user.getRoles();
        return extractScopes(roles);
    }

    /**
     * Filters out any AuthScope instances that don't match the active system/operation.
     */
    protected List<AuthScope> extractScopes(@NonNull List<String> scopeStrs) {
        return scopeStrs.stream().map(s -> AuthScope.from(s))
                .filter(p -> p.matches(scope))
                .collect(Collectors.toList());
    }

    protected boolean validate(@NonNull String uuid5) {
        try {
            UUID.fromString(uuid5);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    void setMetadataService(MetadataService mock) {
        metadataService = mock;
    }

}
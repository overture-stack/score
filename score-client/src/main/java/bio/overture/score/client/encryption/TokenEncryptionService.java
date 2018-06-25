/*
 * Copyright (c) 2018 The Ontario Institute for Cancer Research. All rights reserved.
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

package bio.overture.score.client.encryption;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;


@Slf4j
@Service
public class TokenEncryptionService {
  /*
  Constants
  */
  private static final String KEY_ALGORITHM = "RSA";
  /*
    Dependencies
   */
  @Value("${token.public-key}")
  private String encodedPubKey;

  /*
  Variables
  */
  private Cipher cipher;

  @PostConstruct
  @SneakyThrows
  private void init(){
    val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
    try {
      val decodedPub =  Base64.getDecoder().decode(encodedPubKey);
      X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(decodedPub);
      val publicKey = keyFactory.generatePublic(pubKeySpec);
      cipher = Cipher.getInstance(KEY_ALGORITHM);
      cipher.init(Cipher.PUBLIC_KEY, publicKey);
    } catch (InvalidKeySpecException specEx){
      log.error("Error loading keys:{}", specEx);
    }
  }


  public Optional<String> encryptAccessToken(@NonNull String accessToken){
    try {
      val encryptedBytes = Base64.getEncoder().encode(cipher.doFinal(accessToken.getBytes("UTF-8")));
      return Optional.of(new String(encryptedBytes,"UTF-8"));
    } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e ) {
      log.error("Error encrypting Access token:{}", e);
      return Optional.empty();
    }
  }
}


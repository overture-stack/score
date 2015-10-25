/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.storage.client.cli;

import java.util.Map;

import org.icgc.dcc.storage.client.command.DownloadCommand.OutputLayout;
import org.icgc.dcc.storage.client.command.ViewCommand.OutputType;
import org.icgc.dcc.storage.client.manifest.ManifestResource;
import org.icgc.dcc.storage.client.mount.MountOptions;
import org.icgc.dcc.storage.fs.StorageFileLayout;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public class ConverterFactory implements IStringConverterFactory {

  @Override
  public <T> Class<? extends IStringConverter<T>> getConverter(Class<T> type) {
    if (type.equals(ManifestResource.class)) return cast(ManifestResourceConverter.class);
    return null;
  }

  public static class OutputLayoutConverter extends EnumConverter<OutputLayout> {

    public OutputLayoutConverter(String optionName) {
      super(OutputLayout.class, optionName);
    }

  }

  public static class OutputTypeConverter extends EnumConverter<OutputType> {

    public OutputTypeConverter(String optionName) {
      super(OutputType.class, optionName);
    }

  }

  public static class StorageFileLayoutConverter extends EnumConverter<StorageFileLayout> {

    public StorageFileLayoutConverter(String optionName) {
      super(StorageFileLayout.class, optionName);
    }

  }

  public static class ManifestResourceConverter extends BaseConverter<ManifestResource> {

    public ManifestResourceConverter(String optionName) {
      super(optionName);
    }

    @Override
    public ManifestResource convert(String value) {
      try {
        return new ManifestResource(value);
      } catch (Exception ex) {
        throw new ParameterException(getErrorString(value, "a manifest resource by id, url or file"));
      }
    }

  }

  public static class MountOptionsConverter extends BaseConverter<Map<String, String>> {

    public MountOptionsConverter(String optionName) {
      super(optionName);
    }

    @Override
    public Map<String, String> convert(String value) {
      try {
        return MountOptions.parseOptions(value);
      } catch (Exception ex) {
        throw new ParameterException(getErrorString(value, "mount options"));
      }
    }

  }

  @SuppressWarnings("unchecked")
  private <T> Class<? extends IStringConverter<T>> cast(Class<?> type) {
    return (Class<? extends IStringConverter<T>>) type;
  }

}

"""
delete.py

This script allows developers to delete files generated from icgc-storage
system.
"""


from ConfigParser import SafeConfigParser
from boto.s3.connection import S3Connection, Key
from boto.exception import S3ResponseError
import defaults
import argparse
import os


class ICGCStorageSettings:
    """
    Handles the settings needed to access the S3 buckets used by the
    ICGC Storage System

    Attributes
    -----------
    filename: str
        The location of the config file
    config_parser: ConfigParser.SafeConfigParser
        Reads and writes the config file. Also, contains data of the config
        file from the last read.
    section_default: str
        The name of the default section in the config file
        (Defaults to defaults.CONFIG_SECTION_AWS_KEYS)
    option_name_access_id: str
        The name of the config option that represents the user's
        AWS access id/key
        (Defaults to defaults.DCCOps_OPTION_NAME_ACCESS_ID)
    option_name_secret_key: str
        The name of the config option that represents the user's
        AWS secret access key
        (Defaults to defaults.DCCOps_OPTION_NAME_SECRET_KEY)
    """

    section_default = defaults.CONFIG_SECTION_AWS_KEYS
    option_name_access_id = defaults.DCCOps_OPTION_NAME_ACCESS_ID
    option_name_secret_key = defaults.DCCOps_OPTION_NAME_SECRET_KEY

    def __init__(self, config_file_name):
        """
        The constructor loads the data in settings file from the given
        config_file_name. If no settings file is found, it will create a new
        one with the given config_file_name. If the given settings file
        contains no default section, it will create one.

        Parameters
        ----------
        config_file_name: str
            The location of the settings file.
        """

        self.config_parser = SafeConfigParser()
        self.filename = os.path.realpath(config_file_name)

        if os.path.exists(config_file_name):
            self.config_parser.read(config_file_name)
            self._init_sections()
        else:
            self._create()
            self.config_parser.read(config_file_name)

    def _create(self):
        """
        Creates config file and creates the needed sections
        """
        with open(self.filename, 'w+') as stng_file:
            self.config_parser.write(stng_file)
            self._init_sections()

    def _init_sections(self):
        """
        (Re)initializes the sections in the config
        """
        if not self.config_parser.has_section(
                defaults.CONFIG_SECTION_AWS_KEYS):
            self.config_parser.add_section(defaults.CONFIG_SECTION_AWS_KEYS)
            with open(self.filename, 'w') as cfg_file:
                self.config_parser.write(cfg_file)

    def _update_setting(self, setting_name, val):
        """
        Updates the option/setting with a new value. Then, syncs the values
        from the config file with self.config_parser

        Parameters
        ----------
        setting_name: str
            the name of the option/setting going to be changes
        val: str
            the new value for that option/setting
        """

        self.config_parser.set(self.section_default, setting_name, val)
        with open(self.filename, 'w') as stng_file:
            self.config_parser.write(stng_file)

    def get_access_id(self):
        """
        Gets the AWS access id/key from the config file

        Returns
        -------
        str
            the AWS access id/key from the config file
        """

        return self.config_parser.get(self.section_default,
                                      self.option_name_access_id)

    def set_access_id(self, val):
        """
        Sets an new value for the AWS access id/key from the config file

        Parameters
        ----------
        val: str
            the new value of the AWS access id/key

        """

        self._update_setting(self.option_name_access_id, val)

    def get_secret_key(self):
        """
        Gets the AWS access id/key from the config file

        Returns
        -------
        str
            the AWS access id/key from the config file
        """

        return self.config_parser.get(self.section_default,
                                      self.option_name_secret_key)

    def set_secret_key(self, val):
        """
        Sets an new value for the AWS secret access key from the config file

        Parameters
        ----------
        val: str
            the new value of the AWS secret access key

        """

        self._update_setting(self.option_name_secret_key, val)

    def has_valid_keys(self):
        """
        Checks if self.config_parser has values for all the options needed to
        login AWS.

        Returns
        -------
        boolean
            Is True if the config file has all of the valid AWS keys otherwise
            returns False
        """

        key_option_names = [self.option_name_access_id,
                            self.option_name_secret_key]
        return all([ky_nm in self.config_parser.options(self.section_default)
                    for ky_nm in key_option_names])


class DCCOpsRepo:
    """
    An enumeration representing the repositories used by DCC-Ops
    """

    BOARDWALK = 'boardwalk'
    ACTION_SERVICE = 'action_service'
    REDWOOD = 'redwood'


class DCCOpsSettings:
    """
    Contains all the settings from a given DCC-Ops directory.

    Attributes
    -----------
    dccops_dir: str
        The root directory of the DCC-Ops repository
    _env_vars: dict
        A dictionary of all the environment variables in DCC-Ops
    """

    def __init__(self, dcc_ops_directory):
        """
        Collects the environment variables from Boardwalk,
        Action Service, and Redwood from the DCC-Ops repository.
        Also, initializes attributes.

        Parameters
        ----------
        dcc_ops_directory
            The directory of the DCC-Ops directory
        """

        self.dccops_dir = dcc_ops_directory
        self._env_vars = {}
        self._sync_settings(DCCOpsRepo.BOARDWALK,
                            defaults.DCCOps_BOARDWALK_SUB_DIR)
        self._sync_settings(DCCOpsRepo.ACTION_SERVICE,
                            defaults.DCCOps_ACTION_SERVICE_SUB_DIR)
        self._sync_settings(DCCOpsRepo.REDWOOD,
                            defaults.DCCOps_REDWOOD_SUB_DIR)

    def _sync_settings(self, repo, repo_subdir,
                       env_vars_filename=defaults.DCCOps_ENV_FILENAME):
        """
        Gathers the environment variables from the environment variable file
        of a the given module sub-directory in DCC-Ops.

        This is done by first reading each line in the file. Then, the var name
        and value extracted from the line by splitting the it using
        the "=" character as the delimiter. Afterward, the var name is modified
        by adding to the repo name to prevent conflicts with variables
        with the same name but from different repos. The final key-value pair
        should like the following example.

        Example:
            An environment variable "output_path" is gathered from the
            DCCOpsRepo.REDWOOD repo. It has a value of "root_folder"
            Then, it would have the dictionary entry...

                {"redwood.output_path" : "root_folder"}

        Then, the dictionary entry is added to the dict, self._env_vars.

        Parameters
        ----------
        repo_subdir: str
            the repo's sub-directory containing the
        env_vars_filename: str, optional
            the filename of the environment variable file
        prefix: str, optional
            the prefix of the each variable name for the current repo
            sub-directory
        """

        with open(os.path.join(self.dccops_dir, repo_subdir,
                               env_vars_filename), 'r') as env_file:
            for setting in env_file.readlines():
                if '=' in setting:
                    var_name, var_setting = setting.split('=')
                    var_name = "{}.{}".format(repo, var_name)
                    self._env_vars[var_name] = var_setting

    def get_env_var(self, repo, var_name):
        """
        Gets the value of the environment variable from the given repo and var
        name

        Parameters
        ----------
        repo: DCCOpsRepo, str
            The repo where the environment variable is located
        var_name: str
            The name of the environment variable

        Returns
        -------
        str
            The value of the environment variable from the given repo and var
            name
        """
        return self._env_vars["{}.{}".format(repo, var_name)]


class ICGCDException(Exception):
    """
    Base exception class for ICGCBucketDeleter
    """

    message = None

    def __repr__(self):
        """
        Should have the same functionality as self.__str__()

        Returns
        -------
        str
            output of self.__str__()
        """
        return self.__str__()

    def __str__(self):
        """
        Outputs a formatted error message

        Returns
        -------
        str
            A formatted error message
        """
        return "{}: {}".format(self.__class__.__name__, self.message)


class ICGCDInvalidConfigFileError(ICGCDException):
    """
    Should be thrown if the config file is missing important options
    """
    def __init__(self, file_name=""):
        """
        Initializes error message

        Parameters
        ----------
        file_name: str
            File name of the config file with problems
        """
        self.file_name = file_name
        self.message = "The config file" \
                       " at {} is invalid.".format(self.file_name)


class ICGCDIncompleteError(ICGCDException):
    """
    Should be thrown if file wasn't deleted properly
    """
    def __init__(self, file_uuid=""):
        """
        Initializes error message

        Parameters
        ----------
        file_uuid: str
            File uuid of the file that can't be deleted
        """
        self.file_uuid = file_uuid
        self.message = "Unable to delete File {}." \
                       " File still exists in bucket".format(self.file_uuid)


class ICGCDFileNotFound(ICGCDException):
    """
    Should be thrown if file wasn't found
    """
    def __init__(self, file_uuid=""):
        """
        Initializes error message

        Parameters
        ----------
        file_uuid: str
            File UUID that can't be found
        """
        self.file_uuid = file_uuid
        self.message = "Cannot find the file with the uuid {}." \
                       " The file uuid may be incorrect or the file is not" \
                       " in the bucket.".format(self.file_uuid)


class ICGCBucketDeleter:
    """
    Deletes Files from the AWS S3 buckets used by the ICGC Storage System.
    Also, handles any information related to the file deletion.

    When deleting a file using the ICGCBucketDeleter, the file is deleted and
    then its file_uuid/file_name is added to the redacted_list_file.
    The redacted_list_file located is based on the attribute,
    self.redacted_list_filename.

    Attributes
    ----------
    bucket: boto.s3.connection.Bucket
        The data access object that interfaces with the bucket containing the
        soon-to-be-deleted files
    data_root_folder : str
        The root folder of where all the bundle's files and metadata are saved.
    redacted_list_filename : str
        The location of the redacted list file.
    """

    def __init__(self, bucket_name, storage_settings):
        """
        Connects to AWS using the boto library and the access keys from the
        param, storage_settings. Then, it retrieves the bucket access object
        to initialize the self.bucket attribute

        Parameters
        ----------
        bucket_name: str
            The target file's bucket name
        storage_settings: ICGCStorageSettings
            An ICGCStorageSettings object containing all the setting need to
            access the S3 bucket.

        Raises
        ------
        ICGCDeleteInvalidConfigFile
            The config file is missing important options
        """

        self.bucket = None
        if not storage_settings.has_valid_keys():
            raise ICGCDInvalidConfigFileError(storage_settings.filename)

        connection = S3Connection(storage_settings.get_access_id(),
                                  storage_settings.get_secret_key())

        self.bucket = connection.get_bucket(bucket_name, validate=True)
        self.data_root_folder = defaults.METADATA_FILE_ROOT_FOLDER
        self.redacted_list_filename = defaults.REDACTED_LIST_FILENAME

    def __del__(self):
        """
        Cleans up bucket connection if ICGCBucketDeleter is remove prematurely
        """

        if self.bucket and self.bucket.connection:
            self.bucket.connection.close()

    def delete_file(self, file_uuid):
        """
        Deletes the file based on the file_uuid given. Then, records the file
        uuid in the redacted_list file in the same bucket as the deleted file.

        Parameters
        ----------
        file_uuid: str
            The file_uuid of the soon-to-be deleted file

        Raises
        ------
        ICGCDeleteFileNotFound
            the file isn't found in the bucket
        ICGCDeleteIncompleteError
            the file fails to be deleted
        """

        key = self.get_key_by_file_uuid(file_uuid)
        if not key:
            raise ICGCDFileNotFound(file_uuid)

        self.bucket.delete_key(key)

        if self.get_key_by_file_uuid(file_uuid):
            raise ICGCDIncompleteError
        else:
            self._record_redacted_key(file_uuid)

    def get_key_by_file_uuid(self, file_uuid):
        """
        Gets the boto.s3.connection.Key with the matching file_uuid

        Parameters
        ----------
        file_uuid: str
            The file's uuid


        Returns
        -------
        boto.s3.connection.Key
            The Key of the file with the matching
        """
        return self.bucket.lookup("{}/{}".format(self.data_root_folder,
                                                 file_uuid))

    def _record_redacted_key(self, file_uuid):
        """
        Add the file_uuid of a file thats already been deleted onto the
        redacted list file. If the redacted list file doesn't exists,
        this method creates one.

        Parameters
        ----------
        file_uuid: str
            The file_uuid of the deleted file
        """

        if not self.bucket.lookup(self.redacted_list_filename):
            k = Key(self.bucket)
            k.key = self.redacted_list_filename
            k.set_contents_from_string(file_uuid)
        else:
            k = self.bucket.lookup(self.redacted_list_filename)
            old_string = k.get_contents_as_string()
            new_string = "{}\n{}".format(old_string, file_uuid)
            k.set_contents_from_string(new_string)


def run_cli():
    """
    Initiates the command line interface for admin delete.
    """

    parser = argparse.ArgumentParser()
    sub_parsers = parser.add_subparsers(dest='COMMANDS',
                                        description='Commands')

    icgc_prsr = sub_parsers.add_parser('icgc-delete',
                                       help='Deletes a specific file from the'
                                            ' target S3 bucket')
    icgc_prsr.add_argument('BUCKET_NAME',
                           help='The name of the bucket that contains the'
                                ' target file.')
    icgc_prsr.add_argument('FILE_UUID',
                           help='The file uuid of the file that will be'
                                ' deleted.')

    save_key_prsr = sub_parsers.add_parser('save-access-keys',
                                           help='Saves the AWS access keys'
                                                ' needed for file deletion')
    save_key_prsr.add_argument('S3_ACCESS_ID',
                               help='The S3 Access ID/Key of the AWS Account'
                                    ' with the bucket')
    save_key_prsr.add_argument('S3_SECRET_KEY',
                               help='The S3 Secret Key of the AWS Account with'
                                    ' the bucket')

    load_key_prsr = sub_parsers.add_parser('load-dccops-settings',
                                           help='Loads settings from a dcc-ops'
                                                ' repository')
    load_key_prsr.add_argument('DCC_Ops_DIRECTORY',
                               help='The root directory of the dcc-ops'
                                    ' repository')

    args = parser.parse_args()

    if args.COMMANDS == 'save-access-keys':
        settings = ICGCStorageSettings(defaults.SETTINGS_FILENAME)
        settings.set_access_id(args.S3_ACCESS_ID)
        settings.set_secret_key(args.S3_SECRET_KEY)
        print "Saved your key settings successfully"
    elif args.COMMANDS == 'icgc-delete':
        try:
            deleter = ICGCBucketDeleter(args.BUCKET_NAME,
                                        ICGCStorageSettings(
                                            defaults.SETTINGS_FILENAME)
                                        )
        except ICGCDInvalidConfigFileError as e:
            print str(e)
            print "AWS keys are not loaded yet. Please use the command" \
                  " 'save-access-keys' to load your keys  for this script" \
                  " settings.\nAs an alternative if you have DCC-Ops" \
                  " installed, you can use the command" \
                  " 'load-dccops-settings' in your system and sync your" \
                  " DCC-Ops settings with this script."
        except S3ResponseError as e:
            print str(e)
            print "Please check if your AWS keys are correct."
        else:
            resp = raw_input("Are you sure you want to delete {}?"
                             " [Y]es/[N]o ".format(args.FILE_UUID))

            if resp.lower() in ['y', 'yes']:
                try:
                    deleter.delete_file(args.FILE_UUID)
                except ICGCDIncompleteError as e:
                    print str(e)
                except ICGCDFileNotFound as e:
                    print str(e)
                else:
                    print "Successfully deleted File {}.".format(args.FILE_UUID)
            else:
                print "DID NOT delete File {}.".format(args.FILE_UUID)
    elif args.COMMANDS == 'load-dccops-settings':
        print "Attempting to load your DCC-Ops settings from {}.".format(
            os.path.realpath(args.DCC_Ops_DIRECTORY))
        try:
            env_reader = DCCOpsSettings(args.DCC_Ops_DIRECTORY)
        except IOError as e:
            print "Something is wrong with your DCC-Ops directory path." \
                  " It has the following error..."
            print str(e)
        else:
            settings = ICGCStorageSettings(defaults.SETTINGS_FILENAME)
            settings.set_access_id(
                env_reader.get_env_var(
                    DCCOpsRepo.BOARDWALK,
                    defaults.DCCOps_OPTION_NAME_ACCESS_ID).strip())
            settings.set_secret_key(
                env_reader.get_env_var(
                    DCCOpsRepo.BOARDWALK,
                    defaults.DCCOps_OPTION_NAME_SECRET_KEY).strip())
            print "Loaded your key settings for DCC-Ops successfully."


if __name__ == '__main__':
    run_cli()

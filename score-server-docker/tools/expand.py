import os
import sys
import re

ENV_PATTERN = re.compile(r'\${(\w+)}')


def does_env_exist(env_variable):
    if env_variable in os.environ:
        print("Env \""+env_variable+"\" exists")
        return True
    else:
        return False


def extract_env_vars(line):
    return ENV_PATTERN.findall(line)


def main():
    input_filename = sys.argv[1]
    output_filename = sys.argv[2]
    if not os.path.exists(input_filename):
        raise Exception("The input filename \""+input_filename+"\" DNE")

    print("input_filename ="+input_filename)
    print("output_filename ="+output_filename)
    fo = open(output_filename, 'w')
    has_missing_env_vars = False
    with open(input_filename, "r") as ins:
        for line in ins:
            output_line = line
            for env_variable in extract_env_vars(line):
                if does_env_exist(env_variable):
                    value = os.environ[env_variable]
                    output_line = re.sub(r'\${'+env_variable+'}' , value, output_line)
                else:
                    print ("**ERROR**: ENV variable \""+env_variable+"\" is not defined")
                    has_missing_env_vars = True
            fo.write(output_line)

    fo.close()
    if has_missing_env_vars:
        raise Exception("**ERROR***: there are missing env vars")


if __name__ == "__main__":
    main()

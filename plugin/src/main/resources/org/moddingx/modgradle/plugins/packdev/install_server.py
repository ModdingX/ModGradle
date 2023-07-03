#!/usr/bin/env python3

# ModGradle server install script
# https://github.com/ModdingX/ModGradle

import json
import os
import subprocess
import urllib.parse
from urllib.request import Request, urlopen


def setup_server():
    mods = []
    with open('server.txt') as file:
        for entry in file.read().split('\n'):
            if not entry.strip() == '' and '/' in entry:
                mods.append([entry[:entry.index('/')], entry[entry.index('/') + 1:]])

    try:
        os.remove('run.sh')
        os.remove('run.bat')
    except FileNotFoundError:
        pass

    print('Installing Forge')
    mcv = mods[0][0]
    mlv = mods[0][1]
    request = make_request(f'https://maven.minecraftforge.net/net/minecraftforge/forge/{mcv}-{mlv}/forge-{mcv}-{mlv}-installer.jar')

    response = urlopen(request)
    with open('installer.jar', mode='wb') as file:
        file.write(response.read())

    subprocess.check_call(['java', '-jar', 'installer.jar', '--installServer'])

    try:
        os.remove('installer.jar')
        os.remove('installer.jar.log')
    except FileNotFoundError:
        pass

    if os.path.exists('run.sh'):
        # New installer format 1.17 onwards
        pass
    else:
        # Old installer format before 1.17
        try:
            print('Processing installer output')
            if os.path.exists(f'{mcv}.json'):
                with open(f'{mcv}.json') as file:
                    minecraft_json = json.loads(file.read())
                os.remove(f'{mcv}.json')
                with open(f'{mcv}.json', mode='w') as file:
                    file.write(json.dumps(minecraft_json, indent=4))
        except FileNotFoundError:
            print('Failed to process forge installer output.')

        with open('user_jvm_args.txt', mode='w') as file:
            file.write('# Add custom JVM arguments here\n')

        with open('run.sh', mode='w') as file:
            file.write('#!/usr/bin/env sh\n')
            # Can't use @user_jvm_args.txt here as java 8 doesn't understand it.
            file.write(f'java $(sed -E \'s/^([^#]*)(#.*)?$/\\1/\' user_jvm_args.txt) -jar forge-{mcv}-{mlv}.jar "$@"\n')

        with open('run.bat', mode='w') as file:
            file.write(f'java @user_jvm_args.txt -jar forge-{mcv}-{mlv}.jar %*\n')
            file.write('pause\n')

    print('Adding version specific files')
    if mcv == '1.16.4' or mcv == '1.16.5' or is_major_mc(mcv, '1.17') or is_major_mc(mcv, '1.18'):
        apply_log4j_fix('https://files.minecraftforge.net/log4shell/1.16.4/log4j2_server.xml', 'log4j2_server.xml', no_lookup=not is_major_mc(mcv, '1.16'))
    elif is_major_mc(mcv, '1.13') or is_major_mc(mcv, '1.14') or is_major_mc(mcv, '1.15') or is_major_mc(mcv, '1.16'):
        apply_log4j_fix('https://files.minecraftforge.net/log4shell/1.13/log4j2_server.xml', 'log4j2_server.xml')
    elif is_major_mc(mcv, '1.12'):
        apply_log4j_fix('https://files.minecraftforge.net/log4shell/1.12/log4j2_server.xml', 'log4j2_server.xml')
    elif is_major_mc(mcv, '1.7') or is_major_mc(mcv, '1.8') or is_major_mc(mcv, '1.9') or is_major_mc(mcv, '1.10') or is_major_mc(mcv, '1.11'):
        apply_log4j_fix('https://files.minecraftforge.net/log4shell/1.7/log4j2_server.xml', 'log4j2_server.xml')

    print('Downloading Mods')
    if not os.path.isdir('mods'):
        os.makedirs('mods')
    for mod in mods[1:]:
        attempts = 0
        while True:
            try:
                file_name = mod[0]
                download_url = mod[1]
                print('Downloading mod %s...' % file_name)
                request = make_request(urllib.parse.quote(download_url, safe='/:@?=&'))
                response = urlopen(request)
                with open('mods' + os.path.sep + file_name, mode='wb') as target:
                    target.write(response.read())
                break
            except Exception as e:
                attempts += 1
                if attempts > 10:
                    raise e
                print('Retry download')


def is_major_mc(mcv, expected):
    return mcv == expected or mcv.startswith(expected + '.')


def apply_log4j_fix(file_url, file_name, no_lookup=False):
    download_file(file_url, file_name)
    with open('user_jvm_args.txt', mode='a') as file:
        file.write(f'\n-Dlog4j.configurationFile={file_name}\n')
        if no_lookup:
            file.write('-Dlog4j2.formatMsgNoLookups=true\n')


def download_file(file_url, file_name):
    response = urlopen(make_request(file_url))
    with open(file_name, mode='wb') as file:
        file.write(response.read())


def make_request(file_url):
    return Request(file_url, headers={'Accept': '*/*', 'User-Agent': 'python3/modgradle server installer'})


if __name__ == '__main__':
    setup_server()

#!/usr/bin/env python3

# ModGradle server install script
# https://github.com/noeppi-noeppi/ModGradle

import json
import os
import subprocess
import urllib.parse
from urllib.request import Request, urlopen


def is_major_mc(mcv, expected):
    return mcv == expected or mcv.startswith(expected + '.')


def setup_server():
    mods = []
    with open('server.txt') as file:
        for entry in file.read().split('\n'):
            if not entry.strip() == '':
                mods.append([x.strip() for x in entry.split('/')])

    try:
        os.remove('run.sh')
        os.remove('run.bat')
    except FileNotFoundError:
        pass

    print('Installing Forge')
    mcv = mods[0][0]
    mlv = mods[0][1]
    request = Request(
        f'https://maven.minecraftforge.net/net/minecraftforge/forge/{mcv}-{mlv}/forge-{mcv}-{mlv}-installer.jar')

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

    print("Adding version specific files")
    if is_major_mc(mcv, '1.17') or mcv == '1.18':
        with open('user_jvm_args.txt', mode='a') as file:
            file.write('\n-Dlog4j2.formatMsgNoLookups=true\n')
    elif is_major_mc(mcv, '1.12') or is_major_mc(mcv, '1.13') or is_major_mc(mcv, '1.14') \
            or is_major_mc(mcv, '1.15') or is_major_mc(mcv, '1.16'):
        with open('user_jvm_args.txt', mode='a') as file:
            file.write('\n-Dlog4j.configurationFile=log4j2_112-116.xml\n')
        log_request = Request(
            f'https://launcher.mojang.com/v1/objects/02937d122c86ce73319ef9975b58896fc1b491d1/log4j2_112-116.xml')
        log_response = urlopen(log_request)
        with open('log4j2_112-116.xml', mode='wb') as file:
            file.write(log_response.read())
    elif is_major_mc(mcv, '1.7') or is_major_mc(mcv, '1.8') or is_major_mc(mcv, '1.9') \
            or is_major_mc(mcv, '1.10') or is_major_mc(mcv, '1.11'):
        with open('user_jvm_args.txt', mode='a') as file:
            file.write('\n-Dlog4j.configurationFile=log4j2_17-111.xml\n')
        log_request = Request(
            f'https://launcher.mojang.com/v1/objects/dd2b723346a8dcd48e7f4d245f6bf09e98db9696/log4j2_17-111.xml')
        log_response = urlopen(log_request)
        with open('log4j2_17-111.xml', mode='wb') as file:
            file.write(log_response.read())

    print('Downloading Mods')
    if not os.path.isdir('mods'):
        os.makedirs('mods')
    for mod in mods[1:]:
        project_id = mod[0]
        file_id = mod[1]
        download_url = f'https://addons-ecs.forgesvc.net/api/v2/addon/{project_id}/file/{file_id}/download-url'
        request1 = Request(download_url)
        response1 = urlopen(request1)
        file_url = response1.read().decode('utf-8')
        request2 = Request(urllib.parse.quote(file_url, safe="/:@?=&"))
        response2 = urlopen(request2)
        print('Downloading mod %s...' % file_url[file_url.rfind('/') + 1:])
        with open('mods' + os.path.sep + file_url[file_url.rfind('/') + 1:], mode='wb') as target:
            target.write(response2.read())


if __name__ == '__main__':
    setup_server()

#!/usr/bin/env python3

# ModGradle server install script
# https://github.com/noeppi-noeppi/ModGradle

import json
import os
import subprocess
import urllib.parse
from urllib.request import Request, urlopen


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
    request = Request(f'https://maven.minecraftforge.net/net/minecraftforge/forge/{mcv}-{mlv}/forge-{mcv}-{mlv}-installer.jar')

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

    print('Downloading Mods')
    if not os.path.isdir('mods'):
        os.makedirs('mods')
    for mod in mods[1:]:
        project_id = mod[0]
        file_id = mod[1]
        download_url = f'https://cfa2.cursemaven.com/curse/maven/O-{project_id}/{file_id}/O-{project_id}-{file_id}.jar'
        file_name = get_file_name(project_id, file_id)
        request = Request(urllib.parse.quote(download_url, safe="/:@?=&"), headers={'Accept': 'application/json', 'User-Agent': 'python3/modgradle server installer' })
        response = urlopen(request)
        print('Downloading mod %s...' % file_name)
        with open('mods' + os.path.sep + file_name, mode='wb') as target:
            target.write(response.read())


def get_file_name(project_id, file_id):
    file_info = Request(f'https://curse.melanx.de/project/{project_id}/file/{file_id}', headers={'Accept': 'application/json', 'User-Agent': 'python3/modgradle server installer'})
    data = json.loads(urlopen(file_info).read().decode('utf-8'))
    return data["name"]


if __name__ == '__main__':
    setup_server()

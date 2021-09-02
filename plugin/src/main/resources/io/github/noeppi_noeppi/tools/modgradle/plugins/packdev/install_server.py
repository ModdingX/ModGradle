#!/usr/bin/env python3

import json
import os
import subprocess
import urllib.parse
from urllib.request import Request, urlopen


def download_mods():
    mods = []
    with open('server.txt') as file:
        for entry in file.read().split('\n'):
            if not entry.strip() == '':
                mods.append([x.strip() for x in entry.split('/')])

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
        with open('run.sh') as file:
            content = file.read().rstrip()
        if '--nogui' not in content:
            with open('run.sh', mode='w') as file:
                file.write(content + ' --nogui\n\n')
    else:
        # Old installer format before 1.17
        try:
            print('Renaming installer output')
            os.rename(f'forge-{mcv}-{mlv}.jar', 'forge.jar')
            os.rename(f'minecraft_server.{mcv}.jar', 'minecraft.jar')
            if os.path.exists(f'{mcv}.json'):
                os.rename(f'{mcv}.json', 'minecraft.json')
                with open('minecraft.json') as file:
                    minecraft_json = json.loads(file.read())
                os.remove('minecraft.json')
                with open('minecraft.json', mode='w') as file:
                    file.write(json.dumps(minecraft_json, indent=4))
        except FileNotFoundError:
            print('Failed to rename forge installer output. Forge seems to have changed their installer.')

        with open('run.sh', mode='w') as file:
            file.write('#!/usr/bin/env sh\n')
            file.write('java -jar forge.jar --nogui\n')

        with open('run.bat', mode='w') as file:
            file.write('java -jar forge.jar --nogui\n')
            file.write('pause\n')

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
    download_mods()

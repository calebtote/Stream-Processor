#!/usr/bin/env python
# find . -name "*.java" > sources.txt
# javac -cp .:../lib/kryonet-2.21-all.jar @sources.txt
# jar cvmf ../META-INF/MANIFEST.mf DataStreamer.jar .
# mv DataStreamer.jar ../out/artifacts/
# rm ds.tar
# tar -cf ds.tar
# scp ds.tar root@fa15-cs425-g26-01.cs.illinois.edu:~/testing/

import os
import fabric

APP_HOME = '/home/tote2/artifacts/'
APP_NAME = 'DataStreamer'
JAVA_HOME = '/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/bin/'
ARCHIVE = 'rs.tar'

fabric.api.env.use_ssh_config = True
fabric.api.env.user = 'tote2'

if not fabric.api.env.hosts:
	fabric.api.env.hosts = ['fa15-cs425-g27-01.cs.illinois.edu', 'fa15-cs425-g27-02.cs.illinois.edu', 'fa15-cs425-g27-03.cs.illinois.edu', \
							 'fa15-cs425-g27-05.cs.illinois.edu', 'fa15-cs425-g27-06.cs.illinois.edu', \
							'fa15-cs425-g27-07.cs.illinois.edu']

#fabric.api.env.hosts = ['fa15-cs425-g27-02.cs.illinois.edu']

fabric.api.env.roledefs = {
	'local': []
}

@fabric.api.task
@fabric.decorators.runs_once
@fabric.decorators.roles('local')
def Deploy():
	fabric.api.execute(PrepareDeploy)
	fabric.api.execute(PushDeploy)

	fabric.api.execute(Extract)
	fabric.api.execute(Start)

@fabric.api.task
@fabric.decorators.runs_once
@fabric.decorators.roles('local')
def PrepareDeploy():
	with fabric.context_managers.show('everything'):
		fabric.api.local('find . -name "*.java" > sources.txt')
		fabric.api.local('{0}javac -cp .:../lib/kryonet-2.21-all.jar @sources.txt'.format(JAVA_HOME))
		fabric.api.local('{0}jar cvmf ../META-INF/MANIFEST.mf DataStreamer.jar .'.format(JAVA_HOME))
		fabric.api.local('mv DataStreamer.jar ../out/artifacts/')
		with fabric.context_managers.lcd('{0}/../out/artifacts'.format(os.getcwd())):
			fabric.api.local('rm -f {0}'.format(ARCHIVE))
			fabric.api.local('tar -cf {0} *'.format(ARCHIVE))

@fabric.api.task
@fabric.api.parallel
def PushDeploy():
	fabric.api.put('{0}/../out/artifacts/{1}'.format(os.getcwd(),ARCHIVE), APP_HOME+ARCHIVE)

@fabric.api.task
@fabric.api.parallel
def Extract():
	with fabric.context_managers.cd('{0}'.format(APP_HOME)):
		fabric.api.run('tar -xf {0}'.format(ARCHIVE))

@fabric.api.task
@fabric.api.parallel
def Start():
	with fabric.context_managers.cd('{0}'.format(APP_HOME)):
		with fabric.context_managers.show('everything'):
			fabric.api.run('java -jar {0}.jar'.format(APP_NAME))
			
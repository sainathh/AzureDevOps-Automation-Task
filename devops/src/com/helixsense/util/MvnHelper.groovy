package com.helixsense.util

import java.util.regex.Pattern

def decrementSnapshotVersion(version) {
    String[] v_parts = version.split('-')
    String[] num_parts = v_parts[0].split('\\.')
    int num = Integer.parseInt(num_parts[num_parts.length - 1]) - 1;
    num_parts[num_parts.length - 1] = num.toString()
    return num_parts.join('.') + '-' + v_parts[1]
}

/*
* remove snapshot postfix from main module version,
* and return updated version.
*/
def removeSnapshot() {
    String mvnSet = sh returnStdout: true, script: "mvn versions:set -U -B -Denv=uat -DremoveSnapshot=true"

    def versionPattern = Pattern.compile(
        /^\[INFO]\s+from version ([0-9.]+(-SNAPSHOT)?) to ([0-9.]+(-SNAPSHOT)?)$/)
    
    for (String line : mvnSet.split(/\n/)) {
        def versionMatcher = versionPattern.matcher(line)
        if (versionMatcher.matches()) {
            return versionMatcher.group(3)
        }
    }

    echo "Unable to find the 'to' version"
    return null
}

def setVersionsToRelease(eventPrefix) {
    sh "git checkout ${CommonConstants.release_branch}"
    String releaseVersion = removeSnapshot()
    echo "Release version - ${releaseVersion}"
                
    sh """
        mvn versions:use-releases -U -B -Denv=uat -DfailIfNotReplaced=true
        mvn versions:update-parent -B -Denv=uat -DallowSnapshots=false
        mvn versions:commit -B -Denv=uat
        git diff

        git add .
        git commit -m "[event: ${eventPrefix}-${releaseVersion}]"
    """

    return releaseVersion
}

def updateSnapshotVersion(worksapce, profile_param) { 
    String mvnSet = sh returnStdout: true,
        script: """
            mvn versions:use-latest-versions \\
            -s ${worksapce}/settings.xml \\
            -Denv=${profile_param} -DallowSnapshots=true -U
        """
    
    sh 'mvn versions:commit'
    sh 'git diff'

    def versionPattern = Pattern.compile(
        /^\[INFO]\s+Updated\s+[0-9a-zA-Z.:\-]*\s+to\s+version\s+([0-9.])*-SNAPSHOT$/)
        
    for (String line : mvnSet.split(/\n/)) {
        def versionMatcher = versionPattern.matcher(line)
        
        if (versionMatcher.matches()) {
            return true
        }
    }

    echo "No snapshot updated"
    return false
}

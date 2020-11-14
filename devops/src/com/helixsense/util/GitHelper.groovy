package com.helixsense.util

/*
* @resource could be branch or tag
*/
def canCheckout(module, resource) {
    try {
        String checkoutResult = sh returnStdout: true,
            script: "git checkout ${resource}"
        echo checkoutResult
    } catch (Exception e) {
        echo e.getMessage()
        return false
    }
    return true
}

def getSource(module, branch) {
    gitUrl = "git@github.com:helixsense/${module}.git"
    git branch: "${branch}", changelog: false, credentialsId: 'sshkeyjenkins1', url: "${gitUrl}"
    

    sh """
        git config user.name devops
        git config user.email devops@helixsense.com
        git config push.default current
    """
}

def createShortLivedReleaseBranch(CustomBranchName) {
    echo 'Creating short lived release branch.'
    sh """
        git checkout ${CommonConstants.dev_branch}
        git checkout -b ${CustomBranchName} 
    """
}


def bumpUpSnapshot(event, newVer) {
    echo 'Incrementing version for next development phase.'
    
    sh """
        git checkout ${CommonConstants.dev_branch}
        mvn --batch-mode release:update-versions -DdevelopmentVersion="${newVer}"
        git diff
        git add .
        git commit -m "[event: ${event}] snapshot version incremented."
    """
}

def mergeReleaseToMaster(releaseBranchName) {
    echo 'Merging release to master.'
    merge(releaseBranchName, CommonConstants.master_branch)
}

def mergeToRelease(source) {
    merge(source, CommonConstants.release_branch)
}

def mergeToDevelop(source) {
    merge(source, CommonConstants.dev_branch)
}

def merge(source, destination) {
    sh """
        git checkout ${source}
        git checkout ${destination}
        git merge ${source} --no-ff --no-edit
        git status
    """
}

def tagRelease(tagName) {
    echo "Tagging release - ${tagName}"

    sh """          
        git tag ${tagName}
    """
}

def getCommits(module, branch, tag) {
    try {
        sh "git checkout ${branch}"
        String result = sh returnStdout: true, script: "git cherry -v ${tag}"
        echo "========== Commits to merge =========="
        echo result
        echo "======================================"
        if (result.trim() == "") {
            return null
        } 
        String[] commits = result.trim().split('\n')
        for (int i=0; i < commits.length; i++) {
            commits[i] = commits[i].split(' ')[1]
        }
           
        return commits
    } catch (Exception e) {
        echo e.getMessage()
        return null
    }
}

def applyCommit(branch, commitId) {
    sh """
        git checkout ${branch}
        git cherry-pick ${commitId}
        git status
    """
}

def applyCommits(commits) {
    for (int i=0; i < commits.length; i++) {
        String commitId = commits[i]
        applyCommit(CommonConstants.master_branch, commitId)
        applyCommit(CommonConstants.dev_branch, commitId)
        echo "cherry pick applied for commit - ${commitId}"
    }
}

/*
def createPR(module, tmp_feature_branch) {
    sh """
        echo '' > pr_data.json
        echo '{' >> pr_data.json
        echo '	"title": "use latest version",' >> pr_data.json
        echo '	"description": "dependencies version changes",' >> pr_data.json
        echo '	"source": {' >> pr_data.json
        echo '		"branch": {' >> pr_data.json
        echo '			"name": "${tmp_feature_branch}"' >> pr_data.json
        echo '		}' >> pr_data.json
        echo '	},' >> pr_data.json
        echo '	"destination": {' >> pr_data.json
        echo '		"branch": {' >> pr_data.json
        echo '			"name": "${CommonConstants.dev_branch}"' >> pr_data.json
        echo '		}' >> pr_data.json
        echo '	},' >> pr_data.json
        echo '  "close_source_branch": true' >> pr_data.json
        echo '}' >> pr_data.json
    """
    requestBody = readFile 'pr_data.json'

    httpRequest authentication: 'BitbucketCredential', httpMode: 'POST', outputFile: 'pr_response.txt', 
    requestBody: "${requestBody}", 
    responseHandle: 'NONE', url: "https://bitbucket.org/api/2.0/repositories/lexipol/${module}/pullrequests", 
    validResponseCodes: '100:500'

    response = readFile 'pr_response.txt'
    echo "response: ${response}"

    if (response.contains('Rate limit for this resource') || response.contains('error') || response.contains('Too many invalid password')) {
        return false
    }

    return true
}
*/


@Library('main') _
runCiPipeline(
	jenkinsfile: this,
	projectBaseName: 'core',
	additionalBraintribeArtifactoryRepositoryNames: ['devrock'],
	artifactsWhichNeedToBeRepublishedWhenSolutionListChanges: ['bt-ant-tasks', 'devrock-ant-tasks', 'bt-build-commands', 'model-build-commands'],
	distributionRepositoryName: 'devrock'
)

# Contributing to ICGC DCC

## Getting Started

Software is made better by people like you. If you wish to contribute code to the project we can definitely help you get started. Before continuing please read through this document to ensure that you
understand the expectations set fourth for anyone wishing to make this project better.

* Github is our project source home so please ensure you have a registered Github account before continuing.
* We use a variant of the [Gitflow](https://datasift.github.io/gitflow/index.html)  workflow for our version control management. This means that your
contributions will be managed using [git branching](http://nvie.com/posts/a-successful-git-branching-model/) and Pull Requests.
* Our main stable branch is ```develop``` so please clone and perform your Pull Requests (PRs) towards this branch.

## Cloning the Repository and Performing Pull Requests

* To clone from the ```develop``` you can use the [HubFlow tool](https://datasift.github.io/gitflow/TheHubFlowTools.html) (Gitflow for Github) which will do the fancy footwork for you.
* *Please note* that you do not have to use the HubFlow tool to manage your git workflow. You can also create the ```feature``` and ```hotfix``` branches using the tool (or command line) of your choice. Pull Requests can easily be done through the Github Portal.
Refer to the [Github Pull Request Guide](https://help.github.com/articles/using-pull-requests/) for more details.
* In general PRs come as two predominate types:
  * Bug Fixes - or ```hotfix``` for the git branch prefix
  * Features - or ```feature``` for the git branch prefix
  * Note that we do not use ```release``` branches at this time as implied in the diagram below.
  * In the diagram below ```##repo##``` is ```develop``` in our context.

*Hubflow Workflow Diagram*
![HubFlow Workflow Diagram](https://datasift.github.io/gitflow/GitFlowWorkflowNoFork.png)

Example 1: To create a feature branch (*Step 3*) using Hubflow:
```bash
git hf feature start 3d-gene-renderer
```

Example 2: To create a bugfix branch (*Step 3*) using Hubflow:
```bash
git hf hotfix start 3d-gene-renderer
```

## Coding Conventions

Normalizing your code by following established conventions can help developers understand one another by
reducing the complexity introduced by different coding styles. We have established some code conventions
[here](http://docs.icgc.org/software/development/). Please review them to ensure you are doing your part in keeping this project maintainable.

## Before Opening a Pull Request

When you contribute code, you affirm that the contribution is your original work and that you
license the work to the project under the project's open source license. Whether or not you
state this explicitly, by submitting any copyrighted material via pull request, email, or
other means you agree to license the material under the project's open source license and
warrant that you have the legal authority to do so.

## Additional Resources

* [Using GitFlow With GitHub](https://datasift.github.io/gitflow/GitFlowForGitHub.html)
* [GitFlow Branching Model](http://nvie.com/posts/a-successful-git-branching-model/)

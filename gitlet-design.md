# Gitlet Design Document

**Name**: Cassandra Calciano

##Classes and Data Structures

###Main 
This class will make a call to the methods: init, add, commit, rm, log, 
global-log, checkout, find, status, branch, rm-branch, merge. This class will
contain a folder named COMMIT_FOLDER that contains the tree of all commits. 
There also be a file named BLOBS that contains files that are serializable.

###Staging Area
This will handle all the data structures of the files and the branches.


###Working Directory
A directory will be created within this class. New files will be created here. 
This class will contain on the attributes of a file. Each file will have a status 
that is tracked or untracked. 


## Algorithms

###Main
newCommit(): creates and saves a new commit
init(): creates new working directory to start saving the branches

###Staging Area


###Working Directory


## Persistence


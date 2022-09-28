# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## 2.0 - September/2022
- Updated Angular from 7.2.3 to  14.2.3
- Split the docker container into a separate Backend and frontend containers
- Updated Java 8 to Java 11
- Updated Spring 2.5 to 2.7
    - Introduced Lombok
    - Switched junit4 to junit5
    - switched ascii-docs to swagger-ui
- Split apart unit tests and integration testing 

## 1.2 - November 10/2019
- Added background to main page
- Added Comics:
    - Luann
    - CalvinAndHobbes
    - Pickles
    - Frank-And-Ernest
    - ScaryGary
    - Beetle Bailey
    - Dustin
    - Hagar
    - Mother Goose & Grimm
    - Sherman's Lagoon
    - Zits

## 1.1 - November 9/2019 
- Added support for KingFeatures
- Updated API documentation for previously undocumented methods
- Deprecated Comics
    - Minimum Security
- Added
    - BabyBlues    
- Added Support to Reconcile CacherBootstrapConfig and ComicConfig     

## 1.0
- Save statistics about the Images cached in each top-level comics cache directory to speed up retrieval

## 0.2
- Initial REST api created, exposing method /comics/v1/list
- ComicCacher now maintains the file comics.json

## 0.1 Initial Version
- Caches GoComics
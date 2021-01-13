package com.github.rosecky.shacl2plantuml.lib

interface VocabFilesToPlantUmlFileOptions {

    /**
     * @return Location of the shapes file
     */
    fun getShapesLoc(): String

    /**
     * @return Location of the vocabulary file
     */
    fun getVocabLoc(): String

    /**
     * @return Location of the diagram definition file
     */
    fun getDiagramDefLoc(): String?

    /**
     * @return Output file location
     */
    fun getOutputLoc(): String

    /**
     * @return Desired output format - plantuml | svg now supported
     */
    fun getOutputFormat(): String
}
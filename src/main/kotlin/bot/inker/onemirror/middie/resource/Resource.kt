package bot.inker.onemirror.middie.resource

interface Resource {
    /**
     * Is this a directory
     */
    val isDirectory:Boolean

    /**
     * This resource name
     */
    val name:String

    /**
     * Last modified
     * Unix time stamp
     */
    val lastModified:Long?

    /**
     * File size
     */
    val size:Long?

    /**
     * etag, normal is sha256
     */
    val etag:String?

    /**
     * File list, throw IllegalStateException if not a directory
     */
    val fileList:List<Resource>
}
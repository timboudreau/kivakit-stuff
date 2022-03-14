import com.telenav.kivakit.filesystem.spi.FileSystemService;
import com.telenav.kivakit.filesystems.hdfs.HdfsFileSystemService;

open module kivakit.filesystems.hdfs
{
    provides FileSystemService with HdfsFileSystemService;

    // KivaKit
    requires kivakit.service.client;
    requires kivakit.filesystems.hdfs.proxy.spi;

    // RMI
    requires java.rmi;

    // Module exports
    exports com.telenav.kivakit.filesystems.hdfs;
    exports com.telenav.kivakit.filesystems.hdfs.lexakai;
}

package com.example

/**
 * JNI Bridge for hev-socks5-tunnel.
 * 
 * Provides native methods to start, stop, and get statistics from the tunnel.
 */
class hevsocks5tunnel {
    
    companion object {
        init {
            try {
                System.loadLibrary("hevsocks5tunnel")
            } catch (e: UnsatisfiedLinkError) {
                try {
                    System.loadLibrary("hev-socks5-tunnel")
                } catch (e2: UnsatisfiedLinkError) {
                    e2.printStackTrace()
                }
            }
        }

        /**
         * Starts the TProxy service.
         * 
         * @param configPath Path to the configuration file.
         * @param fd The file descriptor of the TUN interface.
         */
        @JvmStatic
        external fun TProxyStartService(configPath: String, fd: Int)

        /**
         * Stops the TProxy service.
         */
        @JvmStatic
        external fun TProxyStopService()

        /**
         * Gets statistics from the tunnel.
         * 
         * @return A LongArray containing [tx_packets, tx_bytes, rx_packets, rx_bytes].
         */
        @JvmStatic
        external fun TProxyGetStats(): LongArray
    }
}

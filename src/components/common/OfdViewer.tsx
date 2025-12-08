import React, { useEffect, useRef, useState } from 'react'
import { parseOfdDocument, renderOfd } from 'ofd.js'

interface OfdViewerProps {
    fileUrl: string
    className?: string
    style?: React.CSSProperties
}

export function OfdViewer({ fileUrl, className, style }: OfdViewerProps) {
    const containerRef = useRef<HTMLDivElement>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let mounted = true
        setLoading(true)
        setError(null)

        async function loadOfd() {
            try {
                if (!containerRef.current) return

                // Clear previous content
                containerRef.current.innerHTML = ''

                const response = await fetch(fileUrl)
                if (!response.ok) {
                    throw new Error(`Failed to fetch OFD file: ${response.statusText}`)
                }

                const arrayBuffer = await response.arrayBuffer()

                // Use ofd.js to parse
                const ofdDoc = await parseOfdDocument({ ofd: arrayBuffer })

                if (mounted && containerRef.current) {
                    // Render to container
                    const divs = renderOfd(screenWidth, ofdDoc)
                    for (let div of divs) {
                        containerRef.current.appendChild(div)
                    }
                }
            } catch (err: any) {
                console.error("OFD load error:", err)
                if (mounted) {
                    setError(err.message || 'Failed to load OFD file')
                }
            } finally {
                if (mounted) {
                    setLoading(false)
                }
            }
        }

        // Calculate screen width for rendering (or use a fixed width)
        const screenWidth = containerRef.current?.clientWidth || 800

        loadOfd()

        return () => { mounted = false }
    }, [fileUrl])

    return (
        <div className={`ofd-viewer-wrapper ${className || ''}`} style={style}>
            {loading && <div className="p-4 text-center text-gray-500">Loading OFD...</div>}
            {error && <div className="p-4 text-center text-red-500">Error: {error}</div>}
            <div
                ref={containerRef}
                className="ofd-container overflow-auto bg-gray-100 p-4 flex flex-col items-center gap-4"
                style={{ minHeight: '400px' }}
            />
        </div>
    )
}

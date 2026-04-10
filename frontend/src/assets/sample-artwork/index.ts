// Sample artwork from the Metropolitan Museum of Art Open Access collection.
// All works are in the public domain.
// Used for: admin theme live preview panel, e2e test fixtures.

import artwork1 from './artwork-1.jpg'
import artwork2 from './artwork-2.jpg'
import artwork3 from './artwork-3.jpg'
import artwork4 from './artwork-4.jpg'
import artwork5 from './artwork-5.jpg'
import artwork6 from './artwork-6.jpg'

export interface SampleArtwork {
  src: string
  title: string
  artist: string
  metObjectId: number
}

export const sampleArtworks: SampleArtwork[] = [
  { src: artwork1, title: 'A Brazilian Landscape', artist: 'Frans Post', metObjectId: 437323 },
  { src: artwork2, title: 'Still Life with Flowers and Fruit', artist: 'Henri Fantin-Latour', metObjectId: 436293 },
  { src: artwork3, title: 'Still Life with Apples and a Pot of Primroses', artist: 'Paul Cézanne', metObjectId: 435882 },
  { src: artwork4, title: 'Evening: Landscape with an Aqueduct', artist: 'Théodore Géricault', metObjectId: 436455 },
  { src: artwork5, title: 'Still Life with Teapot and Fruit', artist: 'Paul Gauguin', metObjectId: 437999 },
  { src: artwork6, title: 'Still Life with Flowers and Prickly Pears', artist: 'Auguste Renoir', metObjectId: 441104 },
]

// Convenience exports for specific use cases
export const previewHeadliner = sampleArtworks[0]
export const previewTourImages = sampleArtworks.slice(0, 5)

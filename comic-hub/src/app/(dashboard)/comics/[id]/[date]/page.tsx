'use client';

import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ChevronLeft, ChevronRight, ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { useGetComicStripQuery, useGetComicQuery } from '@/generated/graphql';

export default function ComicStripPage() {
  const params = useParams();
  const router = useRouter();
  const comicId = parseInt(params.id as string);
  const date = params.date as string;

  const { data: stripData, isLoading: stripLoading } = useGetComicStripQuery(
    { comicId, date },
  );
  const { data: comicData, isLoading: comicLoading } = useGetComicQuery(
    { id: comicId },
  );

  const strip = stripData?.strip ?? null;
  const comicName = comicData?.comic?.name ?? '';
  const isLoading = stripLoading || comicLoading;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Card>
          <CardContent className="p-6">
            <Skeleton className="w-full aspect-[16/9] mb-4" />
            <div className="flex justify-between">
              <Skeleton className="h-10 w-24" />
              <Skeleton className="h-10 w-24" />
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!strip) {
    return (
      <div className="space-y-6">
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-ink-subtle mb-4">Failed to load comic strip</p>
            <Button onClick={() => router.back()}>Go Back</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!strip.imageUrl || strip.available === false) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Link href="/comics">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-5 w-5" />
            </Button>
          </Link>
          <h1 className="text-2xl font-bold text-ink">{comicName}</h1>
        </div>
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-ink-subtle mb-4">
              No strip available for {new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
            </p>
            <div className="flex justify-center gap-4">
              {strip.previous?.date && (
                <Button
                  variant="outline"
                  onClick={() => router.push(`/comics/${comicId}/${strip.previous!.date}`)}
                >
                  <ChevronLeft className="h-4 w-4 mr-2" />
                  Previous
                </Button>
              )}
              <Button onClick={() => router.push(`/comics/${comicId}`)}>
                View Comic Details
              </Button>
              {strip.next?.date && (
                <Button
                  variant="outline"
                  onClick={() => router.push(`/comics/${comicId}/${strip.next!.date}`)}
                >
                  Next
                  <ChevronRight className="h-4 w-4 ml-2" />
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const formattedDate = new Date(strip.date).toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link href="/comics">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-ink">{comicName}</h1>
          <p className="text-sm text-ink-subtle">{formattedDate}</p>
        </div>
      </div>

      <Card>
        <CardContent className="p-6">
          <div className="bg-canvas rounded-lg mb-6 overflow-hidden">
            <img
              src={strip.imageUrl}
              alt={`${comicName} - ${formattedDate}`}
              className="w-full h-auto"
            />
          </div>

          <div className="flex justify-between items-center">
            <Button
              variant="outline"
              disabled={!strip.previous}
              onClick={() => {
                if (strip.previous?.date) {
                  router.push(`/comics/${comicId}/${strip.previous.date}`);
                }
              }}
            >
              <ChevronLeft className="h-4 w-4 mr-2" />
              Previous
            </Button>

            <Link href={`/comics/${comicId}`}>
              <Button variant="ghost">View Details</Button>
            </Link>

            <Button
              variant="outline"
              disabled={!strip.next}
              onClick={() => {
                if (strip.next?.date) {
                  router.push(`/comics/${comicId}/${strip.next.date}`);
                }
              }}
            >
              Next
              <ChevronRight className="h-4 w-4 ml-2" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

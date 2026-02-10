'use client';

import { useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { Loader2 } from 'lucide-react';
import { loginSchema, type LoginFormData } from '@/lib/validations/auth';
import { useAuth } from '@/hooks/use-auth';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ErrorBanner } from '@/components/auth/error-banner';

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, clearError } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onChange',
    defaultValues: {
      username: '',
      password: '',
      rememberMe: false,
    },
  });

  const { register, handleSubmit, formState: { errors, isValid }, watch } = form;

  const watchedFields = watch();
  const hasInput = watchedFields.username && watchedFields.password;

  const onSubmit = async (data: LoginFormData) => {
    setIsSubmitting(true);
    setErrorMessage(null);
    clearError();

    try {
      await login(data);

      // Redirect to the page they were trying to access, or dashboard
      const redirectTo = searchParams.get('from') || '/';
      router.push(redirectTo);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Login failed';
      setErrorMessage(message);
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="bg-surface shadow-lg">
      <CardHeader className="space-y-1 text-center">
        <div className="flex justify-center mb-4">
          <h1
            className="text-3xl font-bold text-primary"
            style={{ fontFamily: 'var(--font-display)' }}
          >
            Comics Hub
          </h1>
        </div>
        <CardTitle className="text-2xl">Welcome back</CardTitle>
        <CardDescription>
          Sign in to continue to your comic collection
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {errorMessage && (
            <ErrorBanner
              message={errorMessage}
              onDismiss={() => setErrorMessage(null)}
            />
          )}

          <div className="space-y-2">
            <Label htmlFor="username">Username or Email</Label>
            <Input
              id="username"
              type="text"
              placeholder="Enter your username or email"
              autoComplete="username"
              disabled={isSubmitting}
              {...register('username')}
              className={errors.username ? 'border-error' : ''}
            />
            {errors.username && (
              <p className="text-sm text-error">{errors.username.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="password">Password</Label>
              <Link
                href="/forgot-password"
                className="text-sm text-primary hover:text-primary-hover transition-colors"
              >
                Forgot password?
              </Link>
            </div>
            <Input
              id="password"
              type="password"
              placeholder="Enter your password"
              autoComplete="current-password"
              disabled={isSubmitting}
              {...register('password')}
              className={errors.password ? 'border-error' : ''}
            />
            {errors.password && (
              <p className="text-sm text-error">{errors.password.message}</p>
            )}
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="rememberMe"
              disabled={isSubmitting}
              checked={watchedFields.rememberMe}
              onCheckedChange={(checked) => {
                form.setValue('rememberMe', checked === true);
              }}
            />
            <Label
              htmlFor="rememberMe"
              className="text-sm font-normal cursor-pointer"
            >
              Keep me signed in
            </Label>
          </div>

          <Button
            type="submit"
            className="w-full"
            disabled={!hasInput || !isValid || isSubmitting}
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Signing in...
              </>
            ) : (
              'Sign in'
            )}
          </Button>

          <div className="text-center text-sm text-ink-subtle">
            Don't have an account?{' '}
            <Link
              href="/register"
              className="text-primary hover:text-primary-hover font-medium transition-colors"
            >
              Create one
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <Card className="bg-surface shadow-lg">
          <CardContent className="p-8">
            <div className="text-center text-ink-subtle">Loading...</div>
          </CardContent>
        </Card>
      }
    >
      <LoginForm />
    </Suspense>
  );
}
